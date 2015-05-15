package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.security.DigestException;
import java.security.MessageDigest;

import com.salesforce.zsync4j.internal.util.MessageDigestAdapter;
import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;

class Checksum {

  private final MessageDigest digest;
  private final MessageDigestAdapter digestAdapter;
  private final int length;

  // mutable
  private final byte[] bytes;
  private boolean set;

  Checksum(MessageDigest digest, int length) {
    this(digest, new MessageDigestAdapter(digest), length, new byte[digest.getDigestLength()], false);
  }

  private Checksum(MessageDigest digest, MessageDigestAdapter digestAdapter, int length, byte[] bytes, boolean set) {
    this.digest = digest;
    this.digestAdapter = digestAdapter;
    this.length = length;
    this.bytes = bytes;
    this.set = set;
  }

  byte[] getBytes() {
    return bytes;
  }

  int getLength() {
    return length;
  }

  boolean isSet() {
    return set;
  }

  void unset() {
    this.set = false;
  }

  void setChecksum(Checksum other) {
    System.arraycopy(other.bytes, 0, bytes, 0, length);
    set = true;
  }

  void setChecksum(ReadableByteBuffer block) {
    setChecksum(block, 0, block.length() - 1);
  }

  void setChecksum(ReadableByteBuffer buffer, int offset, int length) {
    digest.reset();
    try {
      buffer.write(digestAdapter, offset, length);
      digest.digest(bytes, 0, bytes.length);
    } catch (IOException | DigestException e) {
      throw new RuntimeException("Unexpected error during digest computation", e);
    }
    set = true;
  }

}
