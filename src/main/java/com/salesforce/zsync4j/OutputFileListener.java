/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j;

import static com.google.common.base.Preconditions.*;

import java.net.URI;
import java.nio.file.Path;

public interface OutputFileListener {

  public static final OutputFileListener NO_OP = new OutputFileListener() {

    @Override
    public void transferStarted(OutputFileEvent event) {}

    @Override
    public void bytesDownloaded(OutputFileEvent event) {}

    @Override
    public void bytesWritten(OutputFileEvent event) {}

    @Override
    public void transferEnded(OutputFileEvent event) {}
  };

  void transferStarted(OutputFileEvent event);

  void bytesDownloaded(OutputFileEvent event);

  void bytesWritten(OutputFileEvent event);

  void transferEnded(OutputFileEvent event);

  public static class OutputFileEvent {

    private final Path outputFile;
    private final URI remoteUri;
    private final long remoteFileSizeInBytes;
    private final long bytesDownloaded;
    private final long bytesWritten;
    private final Exception exception;

    public static OutputFileEvent transferStartedEvent(Path outputFile, URI remoteUri, long remoteFileSizeInBytes) {
      return new OutputFileEvent(outputFile, remoteUri, remoteFileSizeInBytes, 0, 0, null);
    }

    public static OutputFileEvent bytesDownloadedEvent(Path outputFile, URI remoteUri, long remoteFileSizeInBytes,
        long bytesDownloaded) {
      return new OutputFileEvent(outputFile, remoteUri, remoteFileSizeInBytes, bytesDownloaded, 0, null);
    }

    public static OutputFileEvent bytesWrittenEvent(Path outputFile, URI remoteUri, long remoteFileSizeInBytes,
        long bytesWritten) {
      return new OutputFileEvent(outputFile, remoteUri, remoteFileSizeInBytes, 0, bytesWritten, null);
    }

    public static OutputFileEvent transferEndedEvent(Path outputFile, URI remoteUri, long remoteFileSizeInBytes) {
      return transferEndedEvent(outputFile, remoteUri, remoteFileSizeInBytes, null);
    }

    public static OutputFileEvent transferEndedEvent(Path outputFile, URI remoteUri, long remoteFileSizeInBytes,
        Exception exception) {
      return new OutputFileEvent(outputFile, remoteUri, remoteFileSizeInBytes, 0, 0, exception);
    }

    private OutputFileEvent(Path outputFile, URI remoteUri, long remoteFileSizeInBytes, long bytesDownloaded,
        long bytesWritten, Exception exception) {
      checkArgument(outputFile != null, "outputFile cannot be null");
      checkArgument(remoteUri != null, "remoteUri cannot be null");
      checkArgument(remoteFileSizeInBytes > 0, "remoteFileSizeInBytes must be greater than 0");
      checkArgument(bytesDownloaded >= 0, "bytesDownloaded must be greater than or equal to 0");
      checkArgument(bytesWritten >= 0, "bytesWritten must be greater than or equal to 0");
      this.outputFile = outputFile;
      this.remoteUri = remoteUri;
      this.remoteFileSizeInBytes = remoteFileSizeInBytes;
      this.bytesDownloaded = bytesDownloaded;
      this.bytesWritten = bytesWritten;
      this.exception = exception;
    }

    public Path getOutputFile() {
      return this.outputFile;
    }

    public URI getRemoteURI() {
      return this.remoteUri;
    }

    public long getRemoteFileSizeInBytes() {
      return this.remoteFileSizeInBytes;
    }

    public long getBytesDownloaded() {
      return this.bytesDownloaded;
    }

    public long getBytesWritten() {
      return this.bytesWritten;
    }

    public Exception getException() {
      return this.exception;
    }
  }
}
