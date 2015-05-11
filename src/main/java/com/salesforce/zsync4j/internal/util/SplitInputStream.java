package com.salesforce.zsync4j.internal.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;

public class SplitInputStream extends InputStream {

  private final InputStream in;
  private final byte[] delimiter;

  // current index to match in delimiter: all positions prior
  private int pos = 0;
  // if we read too far to find delimiter, prepend bytes to next input stream to read them again
  private ByteArrayInputStream prefix;

  public SplitInputStream(InputStream in, byte[] delimiter) {
    this.in = in;
    this.delimiter = delimiter;
  }

  @Override
  public int read() throws IOException {
    if (pos == delimiter.length)
      return -1;
    final int next = in.read();
    if (next == -1)
      return -1;
    if (next == delimiter[pos]) {
      if (++pos == delimiter.length)
        prefix = new ByteArrayInputStream(new byte[0], 0, 0);
    } else {
      pos = 0;
    }
    return next;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (pos == delimiter.length)
      return -1;
    final int read = in.read(b, off, len);
    if (read == -1)
      return -1;
    for (int i = off; i < off + read; i++) {
      if (delimiter[pos] == b[i]) {
        if (++pos == delimiter.length) {
          prefix = new ByteArrayInputStream(b, i, read - (i - pos));
          return i;
        }
      } else {
        pos = 0;
      }
    }
    return read;
  }

  @Override
  public void close() throws IOException {
    in.close();
  }

  public InputStream next() {
    // haven't found delimiter (either not read far enough, or underlying stream exhausted)
    if (prefix == null)
      throw new IllegalStateException("delimiter not reached");
    return new SequenceInputStream(prefix, in);
  }

  int getOffset() {
    return pos;
  }

}
