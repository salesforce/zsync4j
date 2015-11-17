/**
 * Copyright (c) 2015, Salesforce.com, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.zsync.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

/**
 * Efficient rolling window over a byte channel.
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

  /**
   * Constructs a rolling buffer over the given channel. The constructor initializes the buffer by
   * reading at least a full window size from the channel. If the channel does not contain
   * sufficient data to fill a window, an IllegalArgumentException is thrown.
   *
   * @param channel Channel to roll over
   * @param windowSize Size of the window into the channel, must be positive.
   * @param bufferSize Size of the buffer underlying the window, i.e. how many bytes to hold in
   *        memory at any given time. Must be greater than equal to twice the window size.
   * @throws IOException If reading the channel fails
   */
  public RollingBuffer(ReadableByteChannel channel, int windowSize, int bufferSize) throws IOException {
    if (windowSize <= 0 || bufferSize <= 0) {
      throw new IllegalArgumentException("window and buffer size must be positive integers");
    }
    if (bufferSize < 2 * windowSize) {
      throw new IllegalArgumentException("Buffer size must be at least as large as window size");
    }
    if (channel == null) {
      throw new IllegalArgumentException("channel must not be null");
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
   * Advances the window by the given number of bytes.
   *
   * @param bytes Number of bytes to advance the window by. Must the in the interval [0,
   *        windowSize].
   * @return True if window was successfully advanced by the given number of bytes. False,
   *         otherwise, i.e. if the channel does not contain enough bytes to advance the window by
   *         the request number.
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

  /**
   * Returns the length of the window
   */
  @Override
  public int length() {
    return this.length;
  }

  /**
   * Returns the byte at the given index within the current window
   */
  @Override
  public byte get(int i) {
    if (i < 0 || i >= this.length) {
      throw new IndexOutOfBoundsException();
    }
    return this.buffer.get(this.buffer.position() + i);
  }

  /**
   * Writes the current window fully to the given channel
   */
  @Override
  public void write(WritableByteChannel channel) throws IOException {
    write(channel, 0, this.length);
  }

  /**
   * Writes length bytes from the current window starting at the given offset into the channel
   */
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

  /**
   * Ensures sufficient bytes are available in the buffer to satisfy the given request to advance by
   * needed bytes.
   *
   * @param needed Number of bytes needed in the underlying buffer beyond the end of the current
   *        window
   * @return True if the needed number of bytes could be made available, false otherwise.
   * @throws IOException
   */
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

  /**
   * Fills the buffer until it is full or the channel is exhausted. Flips the buffer at the end to
   * make it ready for read.
   *
   * @throws IOException
   */
  void fill() throws IOException {
    do {
      if (this.channel.read(this.buffer) == -1) {
        break;
      }
    } while (this.buffer.hasRemaining());
    this.buffer.flip();
  }

}
