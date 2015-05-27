/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;

import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.ZsyncListener;
import com.salesforce.zsync4j.internal.util.Range;


/**
 * Describe your class here.
 *
 * @author bstclair
 */
public class EventManagerImpl implements EventManager {

  private final ZsyncListener listener;

  private URI requestedZsyncUri;
  private Path outputFile;
  private long startTimeInMilliseconds;
  private long totalBytesDownloaded;
  private String sha1;

  public EventManagerImpl(ZsyncListener listener) {
    this.listener = listener != null ? listener : ZsyncListener.DEFAULT;
  }

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {
    this.startTimeInMilliseconds = System.currentTimeMillis();
    this.listener.transferStarted(requestedZsyncUri, options);
  }

  @Override
  public void controlFileProcessingStarted(URI controlFileUri) {}

  @Override
  public void controlFileProcessingComplete(ControlFile controlFile) {}

  @Override
  public void inputFileProcessingStarted(Path inputFile) {}

  @Override
  public void inputFileProcessingComplete(Path inputFile) {}

  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {}

  @Override
  public void remoteFileProcessingComplete() {}

  @Override
  public void rangeRequestStarted(Iterable<? extends Range> ranges) {}

  @Override
  public void rangeRequestComplete(Iterable<? extends Range> ranges) {}

  @Override
  public void rangeProcessingStarted(Range range) {}

  @Override
  public void rangeProcessingComplete(Range range) {}

  @Override
  public void bytesWritten(Path file, long bytes) {}

  @Override
  public void sha1CalculationStarted(Path file) {}

  @Override
  public void sha1CalculationComplete(String sha1) {
    this.sha1 = sha1;
  }

  @Override
  public void moveTempFileStarted(Path tempFile, Path targetFile) {}

  @Override
  public void moveTempFileComplete() {}

  @Override
  public void zsyncFailed(Exception exception) {
    this.listener.transferFailed(this.requestedZsyncUri, exception);
  }

  @Override
  public void outputFileResolved(Path outputFile) {
    this.outputFile = outputFile;
  }

  @Override
  public void zsyncComplete() {
    long totalTimeInMilliseconds = System.currentTimeMillis() - this.startTimeInMilliseconds;
    this.listener.transferComplete(this.outputFile, this.sha1, this.totalBytesDownloaded, totalTimeInMilliseconds);;
  }

  @Override
  public void transferStarted(String uri, long totalBytes) {}

  @Override
  public void bytesDownloaded(int bytes) {
    this.totalBytesDownloaded += bytes;
  }

  @Override
  public void transferComplete() {}

  @Override
  public void transferClosed() {}

}
