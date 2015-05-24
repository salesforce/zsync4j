/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.salesforce.zsync4j.Zsync.Options;

/**
 * Maintains state relevant for events, and constructs and throws externally published events as
 * necessary.
 *
 * @author bstclair
 */
public interface EventManager {

  void transferStarted(URI requestedZsyncUri, Options options);

  void controlFileDownloadStarted(URI controlFileUri);

  void controlFileDownloadComplete(ControlFile controlFile);

  void inputFileProcessingStarted(Path inputFile);

  void inputFileProcessingComplete(Path inputFile);

  void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests);

  void remoteFileProcessingComplete();

  void blocksRequestStarted(List<Range> blocks);

  void blocksRequestComplete(List<Range> blocks);

  void blockProcessingStarted(Range block);

  void blockProcessingComplete(Range block);

  void bytesDownloaded(long bytes);

  void bytesWritten(Path file, long bytes);

  void sha1CalculationStarted(Path file);

  void sha1CalculationComplete(String sha1);

  void moveTempFileStarted(Path tempFile, Path targetFile);

  void moveTempFileComplete();

  void transferFailed(Exception exception);

  void transferComplete();
}
