package com.salesforce.zsync4j.internal.util;

public class Range {

  // first bit in range
  public final long first;
  // last bit in range
  public final long last;

  public Range(long first, long last) {
    this.first = first;
    this.last = last;
  }

  public long size() {
    return this.last - this.first + 1;
  }

  @Override
  public String toString() {
    return this.first + "-" + this.last;
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + (int) (this.first ^ (this.first >>> 32));
    result = prime * result + (int) (this.last ^ (this.last >>> 32));
    return result;
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
    Range other = (Range) obj;
    if (this.first != other.first) {
      return false;
    }
    if (this.last != other.last) {
      return false;
    }
    return true;
  }

}
