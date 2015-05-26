package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

/**
 * Appends a given number of zeros to a channel by reading them into the buffer once the underlying
 * channel has reached the end of the stream.
 *
 * @author bbusjaeger
 *
 */
public class ZeroPaddedReadableByteChannel implements ReadableByteChannel {

  private final ReadableByteChannel channel;
  private final int zeros;
  int remaining;

  /**
   * Constructs a new padded channel
   *
   * @param channel Channel to pad with zeros
   * @param zeros number of zeros to append to the channel, must be greater or equal to 1
   */
  public ZeroPaddedReadableByteChannel(ReadableByteChannel channel, int zeros) {
    if (channel == null) {
      throw new IllegalArgumentException("underlying channel must not be null");
    }
    if (zeros < 0) {
      throw new IllegalArgumentException("number of zeros must be a positive integer");
    }
    this.channel = channel;
    this.zeros = zeros;
    this.remaining = -1;
  }

  /**
   * Defers open check to underlying channel
   */
  @Override
  public boolean isOpen() {
    return this.channel.isOpen();
  }

  /**
   * closes underlying channel
   */
  @Override
  public void close() throws IOException {
    this.channel.close();
  }

  /**
   * Reads from the underlying channel into the buffer. If the underlying channel has reached the
   * end of stream, 0s are written into the buffer until the buffer is full or no more zeros are
   * remaining. Once the underlying channel and the 0s have all been read, -1 is returned.
   */
  @Override
  public int read(ByteBuffer dst) throws IOException {
    if (this.remaining == -1) {
      int read = this.channel.read(dst);
      if (read == -1) {
        this.remaining = this.zeros;
        return readPadded(dst);
      } else {
        return read;
      }
    } else {
      return readPadded(dst);
    }
  }

  /**
   * If no zeros are remaining, returns -1. Otherwise, reads the minimum of remaining 0s and
   * remaining bytes in the buffer into buffer and returns that number.
   * 
   * @param dst buffer to read into
   * @return
   * @throws IOException
   */
  private int readPadded(ByteBuffer dst) throws IOException {
    if (this.remaining == 0) {
      return -1;
    }
    int min = Math.min(this.remaining, dst.remaining());
    for (int i = 0; i < min; i++) {
      dst.put((byte) 0);
    }
    this.remaining -= min;
    return min;
  }

}
