package com.salesforce.zsync4j.internal;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class BlockSum {

  public static List<BlockSum> read(InputStream in, int numBlocks, int rsumBytes, int checksumBytes) throws IOException {
    in = new BufferedInputStream(in);
    final List<BlockSum> sums = new ArrayList<>(numBlocks);
    for (int i = 0; i < numBlocks; i++)
      sums.add(BlockSum.read(in, rsumBytes, checksumBytes));
    return sums;
  }

  public static BlockSum read(InputStream in, int rsumBytes, int checksumBytes) throws IOException {
    return new BlockSum(read(in, rsumBytes), read(in, checksumBytes));
  }

  private static byte[] read(InputStream in, int len) throws IOException {
    final byte[] b = new byte[len];
    if (in.read(b) != b.length)
      throw new IOException("Failed to read block checksums");
    return b;
  }

  private final byte[] rsum;
  private final byte[] checksum;

  public BlockSum(byte[] rsum, byte[] checksum) {
    this.rsum = rsum;
    this.checksum = checksum;
  }

  public byte[] getRsum() {
    return rsum;
  }

  public byte[] getChecksum() {
    return checksum;
  }

}
