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
package com.salesforce.zsync4j.internal.util;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.SequenceInputStream;
import java.util.Arrays;

/**
 * A utility for reading an InputStream up to a given byte boundary and then resuming the read from
 * that byte boundary using a new InputStream.
 *
 * @author bbusjaeger
 */
public class SplitInputStream extends InputStream {

  private final InputStream in;
  private final byte[] boundary;

  // current index to match in boundary: all positions prior
  private int pos = 0;
  // if we read too far to find boundary, prepend bytes to next input stream to read them again
  private ByteArrayInputStream prefix;

  public SplitInputStream(InputStream in, byte[] boundary) {
    this.in = in;
    this.boundary = boundary;
  }

  /**
   * Tries to read the next byte: Returns -1 if the boundary has been fully read already or the
   * underlying stream is exhausted. Otherwise, it returns the next byte in the stream, keeping
   * track of whether it matches the current position in the boundary.
   */
  @Override
  public int read() throws IOException {
    if (this.pos == this.boundary.length) {
      return -1;
    }
    final int next = this.in.read();
    if (next == -1) {
      return -1;
    }
    if (next == this.boundary[this.pos]) {
      if (++this.pos == this.boundary.length) {
        this.prefix = new ByteArrayInputStream(new byte[0], 0, 0);
      }
    } else {
      this.pos = 0;
    }
    return next;
  }

  /**
   * Tries to read the given number of bytes into the byte array starting at the given offset:
   * Returns -1 if the boundary has been fully read already or the underlying stream is exhausted.
   * Otherwise reads either len bytes or all bytes up to and including the boundary bytes, whatever
   * comes first. Any additional bytes after the boundary that were read to determine the boundary
   * are stored in memory and prepended to the stream returned by {{@link #next()}.
   */
  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    if (this.pos == this.boundary.length) {
      return -1;
    }
    final int read = this.in.read(b, off, len);
    if (read == -1) {
      return -1;
    }
    for (int i = off; i < off + read; i++) {
      if (this.boundary[this.pos] == b[i]) {
        if (++this.pos == this.boundary.length) {
          final int next = i + 1;
          final int r = next - off;
          final byte[] remaining = Arrays.copyOfRange(b, next, next + (read - r));
          this.prefix = new ByteArrayInputStream(remaining);
          return r;
        }
      } else {
        this.pos = 0;
      }
    }
    return read;
  }

  /**
   * If the current stream was read up to (and including) the boundary, returns a new stream that
   * reads bytes starting at the boundary. Otherwise, throws an {@link IllegalStateException}.
   * 
   * @return InputStream starting at boundary after it has been read.
   */
  public InputStream next() {
    // haven't found boundary (either not read far enough, or underlying stream exhausted)
    if (this.prefix == null) {
      throw new IllegalStateException("boundary not reached");
    }
    return new SequenceInputStream(this.prefix, this.in);
  }

}
