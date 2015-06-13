package com.salesforce.zsync4j.http;

/**
 * Content-Range used for http range requests
 * 
 * @author bbusjaeger
 */
public class ContentRange {

  private final long first;
  private final long last;

  public ContentRange(long first, long last) {
    if (first < 0 || last < 0 || first > last) {
      throw new IllegalArgumentException("first and last must be positive with first coming before last");
    }
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
   * Returns how many bytes to return for this range
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
    if (obj == null) {
      return false;
    }
    if (getClass() != obj.getClass()) {
      return false;
    }
    ContentRange other = (ContentRange) obj;
    if (this.first != other.first) {
      return false;
    }
    if (this.last != other.last) {
      return false;
    }
    return true;
  }

  @Override
  public String toString() {
    return this.first + "-" + this.last;
  }

}
