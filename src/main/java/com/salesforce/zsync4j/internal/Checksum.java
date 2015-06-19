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

import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;

import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;
import com.salesforce.zsync4j.internal.util.WritableMessageDigest;

class Checksum {

  private final WritableMessageDigest writableMessageDigest;
  private final int length;

  // mutable
  private final byte[] bytes;
  private boolean set;

  Checksum(MessageDigest digest, int length) {
    this(new WritableMessageDigest(digest), length, new byte[digest.getDigestLength()], false);
  }

  private Checksum(WritableMessageDigest writeableMessageDigest, int length, byte[] bytes, boolean set) {
    this.writableMessageDigest = writeableMessageDigest;
    this.length = length;
    this.bytes = bytes;
    this.set = set;
  }

  byte[] getBytes() {
    return this.bytes;
  }

  int getLength() {
    return this.length;
  }

  boolean isSet() {
    return this.set;
  }

  void unset() {
    this.set = false;
  }

  void setChecksum(Checksum other) {
    System.arraycopy(other.bytes, 0, this.bytes, 0, this.length);
    this.set = true;
  }

  void setChecksum(ReadableByteBuffer block) {
    setChecksum(block, 0, block.length());
  }

  void setChecksum(ReadableByteBuffer buffer, int offset, int length) {
    this.writableMessageDigest.getMessageDigest().reset();
    try {
      buffer.write(this.writableMessageDigest, offset, length);
      this.writableMessageDigest.getMessageDigest().digest(this.bytes, 0, this.bytes.length);
    } catch (IOException | DigestException e) {
      throw new RuntimeException("Unexpected error during digest computation", e);
    }
    this.set = true;
  }

}
