/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j;

import java.net.URI;
import java.nio.file.Path;

import com.salesforce.zsync4j.Zsync.Options;

/**
 * Describe your class here.
 *
 * @author bstclair
 */
public interface ZsyncListener {

  public static final ZsyncListener DEFAULT = new ZsyncListener() {

    @Override
    public void transferStarted(URI controlFileUri, Options options) {}

    @Override
    public void transferComplete(Path outputFile, String sha1, long totalBytesDownloaded,
        long totalTransferTimeInMilliseconds) {}

    @Override
    public void transferFailed(URI controlFileUri, Exception exception) {}
  };

  void transferStarted(URI controlFileUri, Options options);

  void transferComplete(Path outputFile, String sha1, long totalBytesDownloaded, long totalTransferTimeInMilliseconds);

  void transferFailed(URI controlFileUri, Exception exception);
}
