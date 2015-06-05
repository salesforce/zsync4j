package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.ZsyncResults;
import com.salesforce.zsync4j.ZsyncStats;

public class ResultsBuilder extends ZsyncObserver {

  private final Map<Path, Long> contributedBytesByInputFile = new HashMap<>();;

  private long currentContextBytesDownloaded;
  private long startTimeInMilliseconds;
  private long finishedTimeInMilliseconds;
  private long totalBytesDownloaded;
  private Path outputFile;
  private String sha1;
  private long bytesDownloadedForControlFile;
  private long bytesDownloadedFromRemoteTarget;

  @Override
  public void outputFileResolved(Path outputFile) {
    this.outputFile = outputFile;
  }

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {
    this.startTimeInMilliseconds = System.currentTimeMillis();
  }

  @Override
  public void controlFileProcessingStarted(URI controlFileUri) {
    this.currentContextBytesDownloaded = 0;
  }

  @Override
  public void controlFileProcessingComplete(ControlFile controlFile) {
    this.bytesDownloadedForControlFile = this.currentContextBytesDownloaded;
    this.currentContextBytesDownloaded = 0;
  }

  @Override
  public void inputFileProcessingStarted(Path inputFile) {
    this.currentContextBytesDownloaded = 0;
  }

  @Override
  public void inputFileProcessingComplete(Path inputFile) {
    this.contributedBytesByInputFile.put(inputFile, this.currentContextBytesDownloaded);
    this.currentContextBytesDownloaded = 0;
  }

  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {
    this.currentContextBytesDownloaded = 0;
  }

  @Override
  public void remoteFileProcessingComplete() {
    this.bytesDownloadedFromRemoteTarget += this.currentContextBytesDownloaded;
    this.currentContextBytesDownloaded = 0;
  }

  @Override
  public void bytesDownloaded(long bytes) {
    this.totalBytesDownloaded += bytes;
    this.currentContextBytesDownloaded += bytes;
  }

  @Override
  public void sha1Calculated(String sha1) {
    this.sha1 = sha1;
  }

  @Override
  public void zsyncComplete() {
    this.finishedTimeInMilliseconds = System.currentTimeMillis();
  }

  public ZsyncResults build() throws IOException {
    final long totalElapsedMilliseconds = this.finishedTimeInMilliseconds - this.startTimeInMilliseconds;
    final long totalBytesDownloaded = this.totalBytesDownloaded;
    final long outputFileSize = Files.size(this.outputFile);
    final String sha1 = this.sha1;
    final Path outputFile = this.outputFile;
    final long bytesDownloadedForControlFile = this.bytesDownloadedForControlFile;
    final long bytesDownloadedFromRemoteTarget = this.bytesDownloadedFromRemoteTarget;

    final ZsyncStats stats = new ZsyncStats() {

      @Override
      public long getTotalBytesDownloaded() {
        return totalBytesDownloaded;
      }

      @Override
      public long getBytesDownloadedForControlFile() {
        return bytesDownloadedForControlFile;
      }

      @Override
      public long getBytesDownloadedFromRemoteTarget() {
        return bytesDownloadedFromRemoteTarget;
      }

      @Override
      public long getOutputFileSize() {
        return outputFileSize;
      }

      @Override
      public long getTotalElapsedMilliseconds() {
        return totalElapsedMilliseconds;
      }

      @Override
      public Map<Path, Long> getContributedBytesByInputFile() {
        return Collections.unmodifiableMap(ResultsBuilder.this.contributedBytesByInputFile);
      }
    };

    return new ZsyncResults(outputFile, sha1, stats);
  }
}
