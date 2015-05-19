package com.salesforce.zsync4j.internal;

import static com.salesforce.zsync4j.internal.DoubleBlockMatcher.State.INIT;
import static com.salesforce.zsync4j.internal.DoubleBlockMatcher.State.MATCHED_BOTH;
import static com.salesforce.zsync4j.internal.DoubleBlockMatcher.State.MATCHED_FIRST;
import static com.salesforce.zsync4j.internal.DoubleBlockMatcher.State.MISSED;
import static com.salesforce.zsync4j.internal.util.ZsyncUtil.toLong;

import java.security.MessageDigest;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;
import com.salesforce.zsync4j.internal.util.ZsyncUtil;

public class DoubleBlockMatcher extends BlockMatcher {

  static enum State {
    INIT, MISSED, MATCHED_FIRST, MATCHED_BOTH;
  }

  private final int blockSize;
  private final Set<Long> rsumHashSet;

  // mutable state, carried over across invocations
  private State state;
  private final MutableBlockSum currentBlockSum;
  private final MutableBlockSum nextBlockSum;
  private List<Integer> matches;
  private byte firstByte;

  public DoubleBlockMatcher(ControlFile controlFile) {
    final Header header = controlFile.getHeader();
    this.blockSize = header.getBlocksize();

    this.state = INIT;
    final MessageDigest digest = ZsyncUtil.newMD4();
    this.currentBlockSum = new MutableBlockSum(digest, blockSize, header.getRsumBytes(), header.getChecksumBytes());
    this.nextBlockSum = new MutableBlockSum(digest, blockSize, header.getRsumBytes(), header.getChecksumBytes());
    this.rsumHashSet = computeRsumHashSet(controlFile.getBlockSums());
  }

  static Set<Long> computeRsumHashSet(Iterable<? extends BlockSum> blockSums) {
    final ImmutableSet.Builder<Long> b = ImmutableSet.builder();
    final Iterator<? extends BlockSum> it = blockSums.iterator();
    if (it.hasNext()) {
      BlockSum prev = it.next();
      while (it.hasNext()) {
        final BlockSum cur = it.next();
        b.add(toLong(prev.getRsum(), cur.getRsum()));
        prev = cur;
      }
    }
    return b.build();
  }

  @Override
  public int getMatchBytes() {
    return 2 * blockSize;
  }

  @Override
  public int match(OutputFile targetFile, ReadableByteBuffer buffer) {
    switch (state) {
      case INIT:
        // initially we have to compute the rsum from scratch for both blocks
        currentBlockSum.rsum.init(buffer, 0, blockSize);
        nextBlockSum.rsum.init(buffer, blockSize, blockSize);
        matches = tryMatchBoth(targetFile, buffer);

        if (matches.isEmpty()) {
          state = MISSED;
          firstByte = buffer.get(0);
          return 1;
        } else {
          for (int pos : matches)
            targetFile.write(pos, buffer, 0, blockSize);
          state = MATCHED_BOTH;
          return blockSize;
        }
      case MISSED:
        final byte newByte = buffer.get(blockSize - 1);
        currentBlockSum.rsum.update(firstByte, newByte);
        currentBlockSum.checksum.unset();
        nextBlockSum.rsum.update(newByte, buffer.get(buffer.length() - 1));
        nextBlockSum.checksum.unset();

        matches = tryMatchBoth(targetFile, buffer);

        if (matches.isEmpty()) {
          state = MISSED;
          firstByte = buffer.get(0);
          return 1;
        } else {
          for (int pos : matches)
            targetFile.write(pos, buffer, 0, blockSize);
          state = MATCHED_BOTH;
          return blockSize;
        }
      case MATCHED_FIRST:
        currentBlockSum.rsum.init(nextBlockSum.rsum);
        nextBlockSum.rsum.init(buffer, blockSize, blockSize);

        if (nextBlockSum.checksum.isSet()) {
          currentBlockSum.checksum.setChecksum(nextBlockSum.checksum);
          nextBlockSum.checksum.unset();
          matches = tryMatchBoth(targetFile, buffer);
        } else {
          currentBlockSum.checksum.unset();
          nextBlockSum.checksum.unset();
          matches = tryMatchNext(targetFile, buffer);
        }

        if (matches.isEmpty()) {
          state = MISSED;
          firstByte = buffer.get(0);
          return 1;
        } else {
          for (int pos : matches)
            targetFile.write(pos, buffer, 0, blockSize);
          state = MATCHED_BOTH;
          return blockSize;
        }
      case MATCHED_BOTH:
        currentBlockSum.rsum.init(nextBlockSum.rsum);
        currentBlockSum.checksum.setChecksum(nextBlockSum.checksum);
        nextBlockSum.rsum.init(buffer, blockSize, blockSize);
        nextBlockSum.checksum.unset();

        // first write where this block was found last
        for (int p : matches)
          targetFile.write(++p, buffer, 0, blockSize);
        // then update matches and write where it was found together with next block (may overlap)
        matches = tryMatchNext(targetFile, buffer);

        if (matches.isEmpty()) {
          state = MATCHED_FIRST;
        } else {
          for (int p : matches)
            targetFile.write(p, buffer, 0, blockSize);
          state = MATCHED_BOTH;
        }
        return blockSize;
      default:
        throw new RuntimeException("unmatched state");
    }
  }

  private List<Integer> tryMatchBoth(final OutputFile targetFile, final ReadableByteBuffer buffer) {
    final List<Integer> matches;
    final Long r = toLong(currentBlockSum.rsum.toInt(), nextBlockSum.rsum.toInt());
    // cheap negative check followed by more expensive check
    if (rsumHashSet.contains(r)) {
      // need to compute current block sum
      currentBlockSum.checksum.setChecksum(buffer, 0, blockSize);
      matches = tryMatchNext(targetFile, buffer);
    } else {
      matches = Collections.emptyList();
    }
    return matches;
  }

  private List<Integer> tryMatchNext(final OutputFile targetFile, final ReadableByteBuffer buffer) {
    final List<Integer> positions = targetFile.getPositions(currentBlockSum);
    return positions.isEmpty() ? Collections.<Integer>emptyList() : filterMatches(targetFile, buffer, positions);
  }

  private List<Integer> filterMatches(final OutputFile targetFile, ReadableByteBuffer buffer, List<Integer> positions) {
    // optimize common case
    if (positions.size() == 1) {
      return isNextMatch(targetFile, buffer, positions.get(0)) ? positions : Collections.<Integer>emptyList();
    } else {
      final ImmutableList.Builder<Integer> b = ImmutableList.builder();
      for (Integer position : positions)
        if (isNextMatch(targetFile, buffer, position))
          b.add(position);
      return b.build();
    }
  }

  private boolean isNextMatch(OutputFile targetFile, ReadableByteBuffer buffer, Integer position) {
    final Integer next = position + 1;
    if (next == targetFile.getNumBlocks())
      return true;

    final BlockSum nextTargetBlock = targetFile.get(position + 1);
    if (nextTargetBlock.getRsum() == nextBlockSum.rsum.toInt()) {
      // compute next block sum only once
      if (!nextBlockSum.checksum.isSet())
        nextBlockSum.checksum.setChecksum(buffer, blockSize, blockSize);
      return nextTargetBlock.equals(nextBlockSum);
    }
    return false;
  }
}
