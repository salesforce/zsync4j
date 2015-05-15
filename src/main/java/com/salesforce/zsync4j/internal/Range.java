package com.salesforce.zsync4j.internal;

public class Range {

  // first bit in range
  final long first;
  // last bit in range
  final long last;

  public Range(long first, long last) {
    this.first = first;
    this.last = last;
  }

  public long size() {
    return last - first + 1;
  }

  @Override
  public String toString() {
    return first + "-" + last;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (first ^ (first >>> 32));
    result = prime * result + (int) (last ^ (last >>> 32));
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj)
      return true;
    if (obj == null)
      return false;
    if (getClass() != obj.getClass())
      return false;
    Range other = (Range) obj;
    if (first != other.first)
      return false;
    if (last != other.last)
      return false;
    return true;
  }

}
