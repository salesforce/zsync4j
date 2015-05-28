package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Maintains a window view onto the underlying channel. The buffer has only two states: full or
 * empty.
 * 
 * @author bbusjaeger
 *
 */
public class RollingBuffer implements ReadableByteBuffer {

  // the source this buffer provides a view over
  private final ReadableByteChannel channel;
  // a buffer between channel and window to avoid reading one byte at a time
  private final ByteBuffer buffer;
  // length of window
  private final int length;

  public RollingBuffer(ReadableByteChannel channel, int windowSize, int bufferSize) throws IOException {
    if (bufferSize < windowSize) {
      throw new IllegalArgumentException("Buffer size must be at least as large as window size");
    }
    this.channel = channel;
    this.length = windowSize;
    this.buffer = ByteBuffer.allocate(bufferSize);
    fill();
    if (this.buffer.limit() < this.length) {
      throw new IllegalArgumentException("Insufficient bytes available (" + this.buffer.limit()
          + ") to satisfy window size " + this.length);
    }
  }

  /**
   * Advances the buffer to the next available window in the underlying channel. If the current
   * window is empty, a full window will be read from the channel. Otherwise, the window starting at
   * the next byte is read.
   *
   * @return true if sufficient bits were available in the underlying channel to advance the window,
   *         false otherwise
   * @throws IOException
   */
  public boolean advance(int bytes) throws IOException {
    if (bytes < 0) {
      throw new IllegalArgumentException("Cannot advance window backwards");
    }
    if (bytes > this.length) {
      throw new IllegalArgumentException("Cannot advance window beyond current end position");
    }

    if (!ensureBuffered(bytes)) {
      return false;
    }

    this.buffer.position(this.buffer.position() + bytes);
    return true;
  }

  @Override
  public int length() {
    return this.length;
  }

  @Override
  public byte get(int i) {
    if (i < 0 || i > this.length) {
      throw new IndexOutOfBoundsException();
    }
    return this.buffer.get(this.buffer.position() + i);
  }

  @Override
  public void write(WritableByteChannel channel) throws IOException {
    write(channel, 0, this.length);
  }

  @Override
  public void write(WritableByteChannel channel, int offset, int length) throws IOException {
    if (offset < 0 || offset >= this.length) {
      throw new IndexOutOfBoundsException("Invalid offset " + offset);
    }
    if (offset + length > this.length) {
      throw new IndexOutOfBoundsException("Invalid length " + length);
    }

    final int position = this.buffer.position();
    final int limit = this.buffer.limit();
    try {
      // to write only requested range of current window, set position and limit temporarily
      final int tempPosition = this.buffer.position() + offset;
      final int tempLimit = tempPosition + length;
      this.buffer.position(tempPosition);
      this.buffer.limit(tempLimit);
      do {
        channel.write(this.buffer);
      } while (this.buffer.hasRemaining());
    } finally {
      this.buffer.position(position);
      this.buffer.limit(limit);
    }
  }

  boolean ensureBuffered(int needed) throws IOException {
    if (this.buffer.remaining() < this.length + needed) {
      // reached end of file last time: can't read more
      if (this.buffer.capacity() != this.buffer.limit()) {
        return false;
      }
      // otherwise pull more from channel
      this.buffer.compact();
      fill();
      // check that we now have enough bytes
      if (this.buffer.remaining() < this.length + needed) {
        return false;
      }
    }
    return true;
  }

  void fill() throws IOException {
    do {
      if (this.channel.read(this.buffer) == -1) {
        break;
      }
    } while (this.buffer.hasRemaining());
    this.buffer.flip();
  }

}
