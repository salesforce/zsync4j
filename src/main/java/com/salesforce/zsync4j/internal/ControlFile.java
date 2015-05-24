package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.salesforce.zsync4j.internal.util.SplitInputStream;

public class ControlFile {

  public static ControlFile read(final InputStream in, EventManager events) throws IOException {
    final SplitInputStream firstPart = new SplitInputStream(in, new byte[] {'\n', '\n'});
    final Header header = Header.read(firstPart, events);
    final List<? extends BlockSum> blockSums =
        ImmutableBlockSum.readSums(firstPart.next(), header.getNumBlocks(), header.getRsumBytes(),
            header.getChecksumBytes(), events);
    return new ControlFile(header, blockSums);
  }

  private final Header header;
  private final List<? extends BlockSum> blockSums;

  public ControlFile(Header header, List<? extends BlockSum> blockSums) {
    super();
    this.header = header;
    this.blockSums = blockSums;
  }

  public Header getHeader() {
    return this.header;
  }

  public List<? extends BlockSum> getBlockSums() {
    return this.blockSums;
  }

}
