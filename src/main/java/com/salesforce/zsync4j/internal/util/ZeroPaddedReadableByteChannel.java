package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class ZeroPaddedReadableByteChannel implements ReadableByteChannel {

  private final ReadableByteChannel channel;
  private final int length;
  int remaining;

  public ZeroPaddedReadableByteChannel(ReadableByteChannel channel, int length) {
    if (length < 1)
      throw new IllegalArgumentException("Must pad with at least one zero");
    this.channel = channel;
    this.length = length;
    this.remaining = -1;
  }

  @Override
  public boolean isOpen() {
    return channel.isOpen();
  }

  @Override
  public void close() throws IOException {
    channel.close();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (remaining == -1) {
      int read = channel.read(dst);
      if (read == -1) {
        remaining = length;
        return readPadded(dst);
      } else {
        return read;
      }
    } else {
      if (remaining == 0) {
        return -1;
      } else {
        return readPadded(dst);
      }
    }
  }

  private int readPadded(ByteBuffer dst) throws IOException {
    int min = Math.min(remaining, dst.remaining());
    for (int i = 0; i < min; i++)
      dst.put((byte) 0);
    remaining -= min;
    return min;
  }

}