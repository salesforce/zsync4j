package com.salesforce.zsync4j.internal;

import java.io.IOException;

public class MissingRangesIOException extends IOException {

  private static final long serialVersionUID = -1610328921045178201L;

  public MissingRangesIOException(String message) {
    super(message);
  }
}
