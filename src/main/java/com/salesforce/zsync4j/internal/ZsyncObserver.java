/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;

import com.salesforce.zsync4j.Zsync.Options;

/**
 * Observes events over the course of a single zsync invocation.
 *
 * @author bstclair
 */
public class ZsyncObserver {

  public void outputFileResolved(Path outputFile) {}

  public void zsyncStarted(URI requestedZsyncUri, Options options) {}

  public void controlFileProcessingStarted(URI controlFileUri) {}

  public void controlFileProcessingComplete(ControlFile controlFile) {}

  public void inputFileProcessingStarted(Path inputFile) {}

  public void inputFileProcessingComplete(Path inputFile) {}

  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {}

  public void remoteFileProcessingComplete() {}

  public void bytesDownloaded(long bytes) {}

  public void bytesWritten(Path file, long bytes) {}

  public void sha1Calculated(String sha1) {}

  public void zsyncFailed(Exception exception) {}

  public void zsyncComplete() {}
}
