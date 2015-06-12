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
    this.currentBlockSum =
        new MutableBlockSum(digest, this.blockSize, header.getRsumBytes(), header.getChecksumBytes());
    this.nextBlockSum = new MutableBlockSum(digest, this.blockSize, header.getRsumBytes(), header.getChecksumBytes());
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
  public int getMatcherBlockSize() {
    return 2 * this.blockSize;
  }

  @Override
  public int match(OutputFile outputFile, ReadableByteBuffer buffer) {
    switch (this.state) {
      case INIT:
        // initially we have to compute the rsum from scratch for both blocks
        this.currentBlockSum.rsum.init(buffer, 0, this.blockSize);
        this.nextBlockSum.rsum.init(buffer, this.blockSize, this.blockSize);
        this.matches = this.tryMatchBoth(outputFile, buffer);
        return this.matches.isEmpty() ? this.missed(buffer) : this.matchedBoth(outputFile, buffer);
      case MISSED:
        // if we missed last time, update rolling sums by one byte and reset checksums
        final byte newByte = buffer.get(this.blockSize - 1);
        this.currentBlockSum.rsum.update(this.firstByte, newByte);
        this.currentBlockSum.checksum.unset();
        this.nextBlockSum.rsum.update(newByte, buffer.get(buffer.length() - 1));
        this.nextBlockSum.checksum.unset();
        this.matches = this.tryMatchBoth(outputFile, buffer);
        return this.matches.isEmpty() ? this.missed(buffer) : this.matchedBoth(outputFile, buffer);
      case MATCHED_FIRST:
        // if we matched the first block last time, reuse rolling sum for current block
        this.currentBlockSum.rsum.init(this.nextBlockSum.rsum);
        this.nextBlockSum.rsum.init(buffer, this.blockSize, this.blockSize);
        // We will have computed the checksum for the current block if rolling sum check passed
        // If so, reuse the checksum to lookup the block directly and try to match successors
        if (this.nextBlockSum.checksum.isSet()) {
          this.currentBlockSum.checksum.setChecksum(this.nextBlockSum.checksum);
          this.nextBlockSum.checksum.unset();
          this.matches = this.tryMatchNext(outputFile, buffer);
        }
        // Otherwise, try to match a double block based on the combined rolling sum
        else {
          this.currentBlockSum.checksum.unset();
          this.nextBlockSum.checksum.unset();
          this.matches = this.tryMatchBoth(outputFile, buffer);
        }
        return this.matches.isEmpty() ? this.missed(buffer) : this.matchedBoth(outputFile, buffer);
      case MATCHED_BOTH:
        // if we matched both blocks last time, reuse rolling sum and checksum for current block
        this.currentBlockSum.rsum.init(this.nextBlockSum.rsum);
        this.currentBlockSum.checksum.setChecksum(this.nextBlockSum.checksum);
        this.nextBlockSum.rsum.init(buffer, this.blockSize, this.blockSize);
        this.nextBlockSum.checksum.unset();
        // now try to find where current and next match (may overlap with previous matches)
        this.matches = this.tryMatchNext(outputFile, buffer);
        return this.matches.isEmpty() ? this.matchedFirst() : this.matchedBoth(outputFile, buffer);
      default:
        throw new RuntimeException("unmatched state");
    }
  }

  private int missed(ReadableByteBuffer buffer) {
    this.state = MISSED;
    this.firstByte = buffer.get(0);
    return 1;
  }

  private int matchedFirst() {
    this.state = MATCHED_FIRST;
    return this.blockSize;
  }

  private int matchedBoth(OutputFile outputFile, ReadableByteBuffer buffer) {
    for (int p : this.matches) {
      outputFile.writeBlock(p, buffer, 0);
      if (++p != outputFile.getNumBlocks()) {
        outputFile.writeBlock(p, buffer, this.blockSize);
      }
    }
    this.state = MATCHED_BOTH;
    return this.blockSize;
  }

  private List<Integer> tryMatchBoth(final OutputFile outputFile, final ReadableByteBuffer buffer) {
    final List<Integer> matches;
    final Long r = toLong(this.currentBlockSum.rsum.toInt(), this.nextBlockSum.rsum.toInt());
    // cheap negative check followed by more expensive check
    if (this.rsumHashSet.contains(r)) {
      // need to compute current block sum
      this.currentBlockSum.checksum.setChecksum(buffer, 0, this.blockSize);
      matches = this.tryMatchNext(outputFile, buffer);
    } else {
      matches = Collections.emptyList();
    }
    return matches;
  }

  private List<Integer> tryMatchNext(final OutputFile outputFile, final ReadableByteBuffer buffer) {
    final List<Integer> positions = outputFile.getPositions(this.currentBlockSum);
    return positions.isEmpty() ? Collections.<Integer>emptyList() : this.filterMatches(outputFile, buffer, positions);
  }

  private List<Integer> filterMatches(final OutputFile outputFile, ReadableByteBuffer buffer, List<Integer> positions) {
    // optimize common case
    if (positions.size() == 1) {
      return this.isNextMatch(outputFile, buffer, positions.get(0)) ? positions : Collections.<Integer>emptyList();
    } else {
      final ImmutableList.Builder<Integer> b = ImmutableList.builder();
      for (Integer position : positions) {
        if (this.isNextMatch(outputFile, buffer, position)) {
          b.add(position);
        }
      }
      return b.build();
    }
  }

  private boolean isNextMatch(OutputFile outputFile, ReadableByteBuffer buffer, Integer position) {
    final Integer next = position + 1;
    if (next == outputFile.getNumBlocks()) {
      return true;
    }
    final BlockSum nextTargetBlock = outputFile.get(next);
    if (nextTargetBlock.getRsum() == this.nextBlockSum.rsum.toInt()) {
      // compute next block sum only once
      if (!this.nextBlockSum.checksum.isSet()) {
        this.nextBlockSum.checksum.setChecksum(buffer, this.blockSize, this.blockSize);
      }
      return nextTargetBlock.equals(this.nextBlockSum);
    }
    return false;
  }
}
