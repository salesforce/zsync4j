/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j;

import java.net.URI;
import java.nio.file.Path;

public interface OutputFileListener {

  public static final OutputFileListener NO_OP = new OutputFileListener() {

    @Override
    public void transferStarted(Path pathToOutputFile, URI remoteFileUri,
        long remoteFileNumberOfBytes) {

    }

    @Override
    public void bytesDownloaded(long numberOfBytes) {

    }

    @Override
    public void bytesWritten(long numberOfBytes) {

    }

    @Override
    public void transferEnded() {

    }
  };

  void transferStarted(Path pathToOutputFile, URI remoteFileUri, long remoteFileNumberOfBytes);

  void bytesDownloaded(long numberOfBytes);

  void bytesWritten(long numberOfBytes);

  void transferEnded();
}
