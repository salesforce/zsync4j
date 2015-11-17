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
