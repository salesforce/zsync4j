package com.salesforce.zsync4j.http;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Content-Range used for http range requests
 * 
 * @author bbusjaeger
 */
public class ContentRange {

  private final long first;
  private final long last;

  public ContentRange(long first, long last) {
    checkArgument(first >= 0, "first byte position must be positive integer");
    checkArgument(last >= 0, "last byte position must be positive integer");
    checkArgument(first <= last, "last byte position must be greater or equal to first byte position");
    this.first = first;
    this.last = last;
  }

  /**
   * Returns the content offset, i.e. the index of the first byte of content to return.
   *
   * @return
   */
  public long first() {
    return this.first;
  }

  /**
   * Returns the index of the last byte of content to return
   *
   * @return
   */
  public long last() {
    return this.last;
  }

  /**
   * Returns how many bytes are in this range
   *
   * @return
   */
  public long size() {
    return this.last - this.first + 1;
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
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    ContentRange other = (ContentRange) obj;
    return this.first == other.first && this.last == other.last;
  }

  @Override
  public String toString() {
    return this.first + "-" + this.last;
  }

}
