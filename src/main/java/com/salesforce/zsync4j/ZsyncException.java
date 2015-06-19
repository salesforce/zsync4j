package com.salesforce.zsync4j;

/**
 * Indicates an unexpected error during a zsync operation.
 *
 * @author bbusjaeger
 */
public class ZsyncException extends Exception {

  private static final long serialVersionUID = 3379331057624063316L;

  public ZsyncException(String message) {
    super(message);
  }

  public ZsyncException(Throwable cause) {
    super(cause);
  }

  public ZsyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
