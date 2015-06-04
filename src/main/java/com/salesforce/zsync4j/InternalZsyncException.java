package com.salesforce.zsync4j;

/**
 * A {@link ZsyncException} that indicates an unexpected internal state or condition. Analogous to an
 * {@link IllegalStateException}.
 *
 * @author bstclair
 */
public class InternalZsyncException extends ZsyncException {

  private static final long serialVersionUID = 550786787120068944L;

  public InternalZsyncException(String message) {
    super(message);
  }
}
