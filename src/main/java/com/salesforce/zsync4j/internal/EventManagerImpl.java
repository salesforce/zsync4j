/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.ZsyncListener;
import com.salesforce.zsync4j.internal.util.ProgressMonitor;


/**
 * Describe your class here.
 *
 * @author bstclair
 */
public class EventManagerImpl implements EventManager, ProgressMonitor {

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
  public void transferStarted(URI requestedZsyncUri, Options options) {
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
  public void blocksRequestStarted(List<Range> blocks) {}

  @Override
  public void blocksRequestComplete(List<Range> blocks) {}

  @Override
  public void blockProcessingStarted(Range block) {}

  @Override
  public void blockProcessingComplete(Range block) {}

  @Override
  public void bytesDownloaded(long bytes) {
    this.totalBytesDownloaded += bytes;
  }

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
  public void transferFailed(Exception exception) {
    this.listener.transferFailed(this.requestedZsyncUri, exception);
  }

  @Override
  public void outputFileResolved(Path outputFile) {
    this.outputFile = outputFile;
  }

  @Override
  public void transferComplete() {
    long totalTimeInMilliseconds = System.currentTimeMillis() - this.startTimeInMilliseconds;
    this.listener.transferComplete(this.outputFile, this.sha1, this.totalBytesDownloaded, totalTimeInMilliseconds);;
  }

  @Override
  public void begin(long size) {}

  @Override
  public void progress(int bytesTransferred) {
    this.bytesDownloaded(bytesTransferred);
  }

  @Override
  public void done() {}
}
