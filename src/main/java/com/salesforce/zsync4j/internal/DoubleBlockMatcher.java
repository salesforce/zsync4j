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
  public int match(OutputFile outputFile, ReadableByteBuffer buffer) {
    switch (state) {
      case INIT:
        // initially we have to compute the rsum from scratch for both blocks
        currentBlockSum.rsum.init(buffer, 0, blockSize);
        nextBlockSum.rsum.init(buffer, blockSize, blockSize);
        matches = tryMatchBoth(outputFile, buffer);
        return matches.isEmpty() ? missed(buffer) : matchedBoth(outputFile, buffer);
      case MISSED:
        // if we missed last time, update rolling sums by one byte and reset checksums
        final byte newByte = buffer.get(blockSize - 1);
        currentBlockSum.rsum.update(firstByte, newByte);
        currentBlockSum.checksum.unset();
        nextBlockSum.rsum.update(newByte, buffer.get(buffer.length() - 1));
        nextBlockSum.checksum.unset();
        matches = tryMatchBoth(outputFile, buffer);
        return matches.isEmpty() ? missed(buffer) : matchedBoth(outputFile, buffer);
      case MATCHED_FIRST:
        // if we matched the first block last time, reuse rolling sum for current block
        currentBlockSum.rsum.init(nextBlockSum.rsum);
        nextBlockSum.rsum.init(buffer, blockSize, blockSize);
        // We will have computed the checksum for the current block if rolling sum check passed
        // If so, reuse the checksum to lookup the block directly and try to match successors
        if (nextBlockSum.checksum.isSet()) {
          currentBlockSum.checksum.setChecksum(nextBlockSum.checksum);
          nextBlockSum.checksum.unset();
          matches = tryMatchNext(outputFile, buffer);
        }
        // Otherwise, try to match a double block based on the combined rolling sum
        else {
          currentBlockSum.checksum.unset();
          nextBlockSum.checksum.unset();
          matches = tryMatchBoth(outputFile, buffer);
        }
        return matches.isEmpty() ? missed(buffer) : matchedBoth(outputFile, buffer);
      case MATCHED_BOTH:
        // if we matched both blocks last time, reuse rolling sum and checksum for current block
        currentBlockSum.rsum.init(nextBlockSum.rsum);
        currentBlockSum.checksum.setChecksum(nextBlockSum.checksum);
        nextBlockSum.rsum.init(buffer, blockSize, blockSize);
        nextBlockSum.checksum.unset();
        // write out this block where it matched last (position incremented)
        for (int p : matches)
          outputFile.write(++p, buffer, 0, blockSize);
        // now try to find where current and next match (may overlap with previous matches)
        matches = tryMatchNext(outputFile, buffer);
        return matches.isEmpty() ? matchedFirst() : matchedBoth(outputFile, buffer);
      default:
        throw new RuntimeException("unmatched state");
    }
  }

  private int missed(ReadableByteBuffer buffer) {
    state = MISSED;
    firstByte = buffer.get(0);
    return 1;
  }

  private int matchedFirst() {
    state = MATCHED_FIRST;
    return blockSize;
  }

  private int matchedBoth(OutputFile outputFile, ReadableByteBuffer buffer) {
    for (int p : matches)
      outputFile.write(p, buffer, 0, blockSize);
    state = MATCHED_BOTH;
    return blockSize;
  }

  private List<Integer> tryMatchBoth(final OutputFile outputFile, final ReadableByteBuffer buffer) {
    final List<Integer> matches;
    final Long r = toLong(currentBlockSum.rsum.toInt(), nextBlockSum.rsum.toInt());
    // cheap negative check followed by more expensive check
    if (rsumHashSet.contains(r)) {
      // need to compute current block sum
      currentBlockSum.checksum.setChecksum(buffer, 0, blockSize);
      matches = tryMatchNext(outputFile, buffer);
    } else {
      matches = Collections.emptyList();
    }
    return matches;
  }

  private List<Integer> tryMatchNext(final OutputFile outputFile, final ReadableByteBuffer buffer) {
    final List<Integer> positions = outputFile.getPositions(currentBlockSum);
    return positions.isEmpty() ? Collections.<Integer>emptyList() : filterMatches(outputFile, buffer, positions);
  }

  private List<Integer> filterMatches(final OutputFile outputFile, ReadableByteBuffer buffer, List<Integer> positions) {
    // optimize common case
    if (positions.size() == 1) {
      return isNextMatch(outputFile, buffer, positions.get(0)) ? positions : Collections.<Integer>emptyList();
    } else {
      final ImmutableList.Builder<Integer> b = ImmutableList.builder();
      for (Integer position : positions)
        if (isNextMatch(outputFile, buffer, position))
          b.add(position);
      return b.build();
    }
  }

  private boolean isNextMatch(OutputFile outputFile, ReadableByteBuffer buffer, Integer position) {
    final Integer next = position + 1;
    if (next == outputFile.getNumBlocks())
      return true;

    final BlockSum nextTargetBlock = outputFile.get(position + 1);
    if (nextTargetBlock.getRsum() == nextBlockSum.rsum.toInt()) {
      // compute next block sum only once
      if (!nextBlockSum.checksum.isSet())
        nextBlockSum.checksum.setChecksum(buffer, blockSize, blockSize);
      return nextTargetBlock.equals(nextBlockSum);
    }
    return false;
  }
}
