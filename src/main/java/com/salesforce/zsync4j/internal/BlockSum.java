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
    if (this == obj) {
      return true;
    }
    if (obj == null) {
      return false;
    }
    if (!(obj instanceof BlockSum)) {
      return false;
    }
    BlockSum other = (BlockSum) obj;
    if (getRsum() != other.getRsum()) {
      return false;
    }
    if (!equals(getChecksum(), getChecksumLength(), other.getChecksum(), other.getChecksumLength())) {
      return false;
    }
    return true;
  }

  private static int hashCode(byte a[], int length) {
    if (a == null) {
      return 0;
    }
    int result = 1;
    for (int i = 0; i < length; i++) {
      final byte element = a[i];
      int elementHash = element ^ (element >>> 32);
      result = 31 * result + elementHash;
    }
    return result;
  }

  private static boolean equals(byte[] a, int length, byte[] a2, int legnth2) {
    if (length != legnth2) {
      return false;
    }
    if (a == a2) {
      return true;
    }
    if (a == null || a2 == null) {
      return false;
    }
    for (int i = 0; i < length; i++) {
      if (a[i] != a2[i]) {
        return false;
      }
    }
    return true;
  }

}
