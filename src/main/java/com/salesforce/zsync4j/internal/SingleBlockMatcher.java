package com.salesforce.zsync4j.internal;

import static com.salesforce.zsync4j.internal.BlockSum.getRsum;
import static com.salesforce.zsync4j.internal.SingleBlockMatcher.State.INIT;
import static com.salesforce.zsync4j.internal.SingleBlockMatcher.State.MATCHED;
import static com.salesforce.zsync4j.internal.SingleBlockMatcher.State.MISSED;
import static com.salesforce.zsync4j.internal.util.ZsyncUtil.newMD4;

import java.util.List;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Iterables;
import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;

public class SingleBlockMatcher extends BlockMatcher {

  static enum State {
    INIT, MATCHED, MISSED;
  }

  private final int blockSize;
  private final Set<Integer> rsumHashSet;

  private State state;
  private MutableBlockSum blockSum;
  private byte firstByte;

  public SingleBlockMatcher(ControlFile controlFile) {
    final Header header = controlFile.getHeader();
    this.blockSize = header.getBlocksize();
    this.rsumHashSet = ImmutableSet.copyOf(Iterables.transform(controlFile.getBlockSums(), getRsum));
    this.state = INIT;
    this.blockSum = new MutableBlockSum(newMD4(), this.blockSize, header.getRsumBytes(), header.getChecksumBytes());
  }

  @Override
  public int getMatcherBlockSize() {
    return this.blockSize;
  }

  @Override
  public int match(OutputFileWriter targetFile, ReadableByteBuffer buffer) {
    switch (this.state) {
      case INIT:
        this.blockSum.rsum.init(buffer);
        break;
      case MATCHED:
        this.blockSum.rsum.init(buffer);
        break;
      case MISSED:
        this.blockSum.rsum.update(this.firstByte, buffer.get(buffer.length() - 1));
        break;
      default:
        throw new RuntimeException("Unhandled case");
    }

    final int r = this.blockSum.rsum.toInt();
    // cheap negative check followed by more expensive positive check
    if (this.rsumHashSet.contains(r)) {
      // only compute strong checksum if weak matched some block
      this.blockSum.checksum.setChecksum(buffer);
      final List<Integer> matches = targetFile.getPositions(this.blockSum);
      if (!matches.isEmpty()) {
        for (Integer position : matches) {
          targetFile.writeBlock(position, buffer);
        }
        this.state = MATCHED;
        return this.blockSize;
      }
    }
    this.state = MISSED;
    this.firstByte = buffer.get(0);
    return 1;
  }

}
