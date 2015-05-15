package com.salesforce.zsync4j.internal;

import java.security.MessageDigest;


class MutableBlockSum extends BlockSum {

  final Rsum rsum;
  final Checksum checksum;

  MutableBlockSum(MessageDigest digest, int blockSize, int rsumLength, int checksumLength) {
    this(new Rsum(rsumLength, blockSize), new Checksum(digest, checksumLength));
  }

  MutableBlockSum(Rsum rsum, Checksum checksum) {
    this.rsum = rsum;
    this.checksum = checksum;
  }

  @Override
  int getRsum() {
    return rsum.toInt();
  }

  @Override
  byte[] getChecksum() {
    return checksum.getBytes();
  }

  @Override
  int getChecksumLength() {
    return checksum.getLength();
  }

}
