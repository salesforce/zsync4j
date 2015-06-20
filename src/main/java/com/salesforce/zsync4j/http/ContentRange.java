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
  public long length() {
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
