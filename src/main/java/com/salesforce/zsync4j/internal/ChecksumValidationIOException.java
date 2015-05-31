package com.salesforce.zsync4j.internal;

import java.io.IOException;

public class ChecksumValidationIOException extends IOException {

  private static final long serialVersionUID = -1436638839738512023L;

  private final String expectedChecksum;
  private final String actualChecksum;

  public ChecksumValidationIOException(String expectedChecksum, String actualChecksum) {
    this.expectedChecksum = expectedChecksum;
    this.actualChecksum = actualChecksum;
  }

  public String getExpectedChecksum() {
    return this.expectedChecksum;
  }

  public String getActualChecksum() {
    return this.actualChecksum;
  }
}
