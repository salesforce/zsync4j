/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;

import com.salesforce.zsync4j.OutputFileObserver;
import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.internal.util.Range;


/**
 * Describe your class here.
 *
 * @author bstclair
 */
public class EventDispatcherImpl implements EventDispatcher {

  private OutputFileObserver outputFileObserver;
  private boolean writingToOutputFile;

  public EventDispatcherImpl() {}

  public void setOutputFileObserver(OutputFileObserver outputFileObserver) {
    this.outputFileObserver = outputFileObserver;
  }

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {}

  @Override
  public void controlFileProcessingStarted(URI controlFileUri) {}

  @Override
  public void controlFileProcessingComplete(ControlFile controlFile) {
    if (this.outputFileObserver != null) {
      this.outputFileObserver.contentLengthAvailable(controlFile.getHeader().getLength());
    }
  }

  @Override
  public void inputFileProcessingStarted(Path inputFile) {
    this.writingToOutputFile = true;
  }

  @Override
  public void inputFileProcessingComplete(Path inputFile) {
    this.writingToOutputFile = false;
  }

  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {}

  @Override
  public void remoteFileProcessingComplete() {}

  @Override
  public void rangeRequestStarted(Iterable<? extends Range> ranges) {}

  @Override
  public void rangeRequestComplete(Iterable<? extends Range> ranges) {

  }

  @Override
  public void rangeProcessingStarted(Range range) {
    this.writingToOutputFile = true;
  }

  @Override
  public void rangeProcessingComplete(Range range) {
    this.writingToOutputFile = false;
  }

  @Override
  public void bytesWritten(Path file, long bytes) {
    if (this.writingToOutputFile) {
      if (this.outputFileObserver != null) {
        this.outputFileObserver.bytesWritten(bytes);
      }
    }
  }

  @Override
  public void sha1CalculationStarted(Path file) {}

  @Override
  public void sha1CalculationComplete(String sha1) {}

  @Override
  public void moveTempFileStarted(Path tempFile, Path targetFile) {}

  @Override
  public void moveTempFileComplete() {}

  @Override
  public void zsyncFailed(Exception exception) {
    this.writingToOutputFile = false;
  }

  @Override
  public void outputFileResolved(Path outputFile) {
    if (this.outputFileObserver != null) {
      this.outputFileObserver.pathAvailable(outputFile);
    }
  }

  @Override
  public void zsyncComplete() {
    if (this.outputFileObserver != null) {
      this.outputFileObserver.done();
    }
  }

  @Override
  public void transferStarted(String uri, long totalBytes) {}

  @Override
  public void transferProgressed(int bytes) {}

  @Override
  public void transferComplete() {}

  @Override
  public void transferClosed() {}

  @Override
  public void transferFailed(IOException exception) {}

}
