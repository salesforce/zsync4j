package com.salesforce.zsync4j;

import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * An exception indicating that a zsync control file could not be found.
 * 
 * @author bbusjaeger
 *
 */
public class ZsyncFileNotFoundException extends IOException {

  private static final long serialVersionUID = -4355008458949773385L;

  public ZsyncFileNotFoundException(String message, FileNotFoundException cause) {
    super(message, cause);
  }

}
