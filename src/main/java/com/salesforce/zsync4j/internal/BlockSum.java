package com.salesforce.zsync4j.internal;

import com.google.common.base.Function;

abstract class BlockSum {

  static final Function<BlockSum, Integer> getRsum = new Function<BlockSum, Integer>() {
    @Override
    public Integer apply(BlockSum input) {
      return input.getRsum();
    }
  };

  abstract int getRsum();

  abstract byte[] getChecksum();

  abstract int getChecksumLength();

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + hashCode(getChecksum(), getChecksumLength());;
    result = prime * result + getRsum();
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (!(obj instanceof BlockSum))
      return false;
    BlockSum other = (BlockSum) obj;
    if (getRsum() != other.getRsum())
      return false;
    if (!equals(getChecksum(), getChecksumLength(), other.getChecksum(), other.getChecksumLength()))
      return false;
    return true;
  }

  private static int hashCode(byte a[], int length) {
    if (a == null)
      return 0;
    int result = 1;
    for (int i = 0; i < length; i++) {
      final byte element = a[i];
      int elementHash = (int) (element ^ (element >>> 32));
      result = 31 * result + elementHash;
    }
    return result;
  }

  private static boolean equals(byte[] a, int length, byte[] a2, int legnth2) {
    if (length != legnth2)
      return false;
    if (a == a2)
      return true;
    if (a == null || a2 == null)
      return false;
    for (int i = 0; i < length; i++)
      if (a[i] != a2[i])
        return false;
    return true;
  }

}