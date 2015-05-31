package com.salesforce.zsync4j;

import java.io.FileNotFoundException;

/**
 * An exception indicating that a zsync control file could not be found.
 *
 * @author bbusjaeger
 *
 */
public class ZsyncControlFileNotFoundException extends ZsyncException {

  private static final long serialVersionUID = -4355008458949773385L;

  public ZsyncControlFileNotFoundException(String message, FileNotFoundException cause) {
    super(message, cause);
  }

}
