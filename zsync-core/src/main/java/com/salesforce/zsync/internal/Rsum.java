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
package com.salesforce.zsync.internal;

import com.salesforce.zsync.internal.util.ReadableByteBuffer;

class Rsum {

  private static int computeBlockShift(int blocksize) {
    for (int i = 0; i < 32; i++) {
      if ((1 << i) == blocksize) {
        return i;
      }
    }
    throw new IllegalArgumentException("Blocksize " + blocksize + " not a power of 2");
  }

  private static short unsigned(byte b) {
    return (short) (b < 0 ? b & 0xFF : b);
  }

  private final int bitmask;
  private final int blockShift;

  public short a;
  public short b;

  Rsum(int length, int blockSize) {
    this.bitmask = (4 == length ? 0xffffffff : 3 == length ? 0xffffff : 2 == length ? 0xffff : 1 == length ? 0xff : 0);
    this.blockShift = computeBlockShift(blockSize);
    this.a = 0;
    this.b = 0;
  }

  void init(Rsum rsum) {
    this.a = rsum.a;
    this.b = rsum.b;
  }

  void init(ReadableByteBuffer buffer) {
    init(buffer, 0, buffer.length());
  }

  void init(ReadableByteBuffer buffer, int offset, int length) {
    this.a = 0;
    this.b = 0;
    for (int i = 0, l = length; i < length; i++, l--) {
      final short val = unsigned(buffer.get(i + offset));
      this.a += val;
      this.b += l * val;
    }
  }

  void update(byte o, byte n) {
    this.a += (unsigned(n) - unsigned(o));
    this.b += this.a - (unsigned(o) << this.blockShift);
  }

  public int toInt() {
    return ((this.a << 16) | (this.b & 0xffff)) & this.bitmask;
  }

  @Override
  public int hashCode() {
    return toInt();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    Rsum other = (Rsum) obj;
    return toInt() == other.toInt();
  }

}
