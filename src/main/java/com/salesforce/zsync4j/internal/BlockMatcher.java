package com.salesforce.zsync4j.internal;

import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;

public abstract class BlockMatcher {

  public static BlockMatcher create(ControlFile controlFile) {
    return controlFile.getHeader().isSeqMatches() ? new DoubleBlockMatcher(controlFile) : new SingleBlockMatcher(
        controlFile);
  }

  public abstract int getMatcherBlockSize();

  public abstract int match(OutputFile targetFile, ReadableByteBuffer data);

}
