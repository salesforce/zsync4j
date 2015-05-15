package com.salesforce.zsync4j.internal;

import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;

class Rsum {

  private static int computeBlockShift(int blocksize) {
    for (int i = 0; i < 32; i++)
      if ((1 << i) == blocksize)
        return i;
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
    a = rsum.a;
    b = rsum.b;
  }

  void init(ReadableByteBuffer buffer) {
    init(buffer, 0, buffer.length());
  }

  void init(ReadableByteBuffer buffer, int offset, int length) {
    a = 0;
    b = 0;
    for (int i = 0, l = length; i < length; i++, l--) {
      final short val = unsigned(buffer.get(i + offset));
      a += val;
      b += l * val;
    }
  }

  void update(byte o, byte n) {
    a += (unsigned(n) - unsigned(o));
    b += a - (unsigned(o) << blockShift);
  }

  public int toInt() {
    return ((a << 16) | (b & 0xffff)) & bitmask;
  }

  @Override
  public int hashCode() {
    return toInt();
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Rsum other = (Rsum) obj;
    return toInt() == other.toInt();
  }

}
