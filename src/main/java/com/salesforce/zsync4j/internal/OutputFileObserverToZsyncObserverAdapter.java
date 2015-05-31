package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;

import com.salesforce.zsync4j.OutputFileObserver;
import com.salesforce.zsync4j.Zsync.Options;

public class OutputFileObserverToZsyncObserverAdapter implements ZsyncObserver {

  private final OutputFileObserver delegate;
  private boolean doneProcessingControlFile;

  public OutputFileObserverToZsyncObserverAdapter(OutputFileObserver delegate) {
    this.delegate = delegate;
  }

  @Override
  public void outputFileResolved(Path outputFile) {
    this.delegate.pathAvailable(outputFile);
  }

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {}

  @Override
  public void controlFileProcessingStarted(URI controlFileUri) {}

  @Override
  public void controlFileProcessingComplete(ControlFile controlFile) {
    this.doneProcessingControlFile = true;
    this.delegate.contentLengthAvailable(controlFile.getHeader().getLength());
  }

  @Override
  public void inputFileProcessingStarted(Path inputFile) {}

  @Override
  public void inputFileProcessingComplete(Path inputFile) {}

  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {}

  @Override
  public void remoteFileProcessingComplete() {}

  @Override
  public void bytesDownloaded(long bytes) {}

  @Override
  public void bytesWritten(Path file, long bytes) {
    if (this.doneProcessingControlFile) {
      this.delegate.bytesWritten(bytes);
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
    this.delegate.failed(exception);
  }

  @Override
  public void zsyncComplete() {
    this.delegate.done();
  }
}
