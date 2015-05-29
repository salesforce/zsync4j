package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;

import com.salesforce.zsync4j.internal.util.MessageDigestAdapter;
import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;

class Checksum {

  private final MessageDigestAdapter digest;
  private final int length;

  // mutable
  private final byte[] bytes;
  private boolean set;

  Checksum(MessageDigest digest, int length) {
    this(new MessageDigestAdapter(digest), length, new byte[digest.getDigestLength()], false);
  }

  private Checksum(MessageDigestAdapter digest, int length, byte[] bytes, boolean set) {
    this.digest = digest;
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
    this.digest.reset();
    try {
      buffer.write(this.digest, offset, length);
      this.digest.digest(this.bytes, 0, this.bytes.length);
    } catch (IOException | DigestException e) {
      throw new RuntimeException("Unexpected error during digest computation", e);
    }
    this.set = true;
  }

}
