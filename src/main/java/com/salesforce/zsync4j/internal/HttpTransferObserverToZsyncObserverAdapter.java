package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;

import com.salesforce.zsync4j.HttpTransferObserver;
import com.salesforce.zsync4j.Zsync.Options;

public class HttpTransferObserverToZsyncObserverAdapter implements ZsyncObserver {

  private final HttpTransferObserver delegate;

  private boolean transferInProgress;

  public HttpTransferObserverToZsyncObserverAdapter(HttpTransferObserver delegate) {
    this.delegate = delegate;
  }

  @Override
  public void outputFileResolved(Path outputFile) {}

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {}

  @Override
  public void controlFileProcessingStarted(URI controlFileUri) {
    if (controlFileUri.toString().startsWith("http")) {

    }
  }

  @Override
  public void controlFileProcessingComplete(ControlFile controlFile) {}

  @Override
  public void inputFileProcessingStarted(Path inputFile) {}

  @Override
  public void inputFileProcessingComplete(Path inputFile) {}

  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {
    this.transferInProgress = true;
    this.delegate.transferInitiated(remoteUri, expectedBytes);
  }

  @Override
  public void remoteFileProcessingComplete() {
    this.transferInProgress = false;
    this.delegate.transferFinished();
  }

  @Override
  public void bytesDownloaded(long bytes) {
    if (this.transferInProgress) {
      this.delegate.bytesTransferred(bytes);
    }
  }

  @Override
  public void bytesWritten(Path file, long bytes) {
    // TODO Auto-generated method stub

  }

  @Override
  public void sha1CalculationStarted(Path file) {
    // TODO Auto-generated method stub

  }

  @Override
  public void sha1CalculationComplete(String sha1) {
    // TODO Auto-generated method stub

  }

  @Override
  public void moveTempFileStarted(Path tempFile, Path targetFile) {
    // TODO Auto-generated method stub

  }

  @Override
  public void moveTempFileComplete() {
    // TODO Auto-generated method stub

  }

  @Override
  public void zsyncFailed(Exception exception) {
    // TODO Auto-generated method stub

  }

  @Override
  public void zsyncComplete() {
    // TODO Auto-generated method stub

  }
}
