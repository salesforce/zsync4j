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
