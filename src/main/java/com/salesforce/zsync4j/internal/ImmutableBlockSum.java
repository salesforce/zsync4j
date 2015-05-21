package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ImmutableBlockSum extends BlockSum {

  public static List<ImmutableBlockSum> readSums(InputStream in, int numBlocks, int rsumBytes, int checksumBytes) throws IOException {
    final ImmutableList.Builder<ImmutableBlockSum> b = ImmutableList.builder();
    for (int i = 0; i < numBlocks; i++)
      b.add(ImmutableBlockSum.read(in, rsumBytes, checksumBytes));
    return b.build();
  }

  public static ImmutableBlockSum read(InputStream in, int rsumBytes, int checksumBytes) throws IOException {
    return new ImmutableBlockSum(readRsum(in, rsumBytes), readChecksum(in, checksumBytes));
  }

  static int readRsum(InputStream in, int rsumBytes) throws IOException {
    int rsum = 0;
    for (int i = rsumBytes - 1; i >= 0; i--) {
      int next = in.read();
      if (next == -1)
        throw new IllegalArgumentException("Failed to read rsum: premature end of file");
      rsum |= next << (i * 8);
    }
    return rsum;
  }

  static byte[] readChecksum(InputStream in, int len) throws IOException {
    final byte[] b = new byte[len];
    int read = 0;
    int r;
    while (read < len && (r = in.read(b, read, len - read)) != -1)
      read += r;
    if (read != b.length)
      throw new IOException("Failed to read block checksums");
    return b;
  }


  private final int rsum;
  private final byte[] checksum;

  private volatile Integer hashCode;

  public ImmutableBlockSum(int rsum, byte[] checksum) {
    this.rsum = rsum;
    this.checksum = checksum;
  }

  @Override
  int getRsum() {
    return rsum;
  }

  @Override
  byte[] getChecksum() {
    return checksum;
  }

  @Override
  int getChecksumLength() {
    return checksum.length;
  }

  @Override
  public int hashCode() {
    if (hashCode == null)
      hashCode = super.hashCode();
    return hashCode;
  }
}
