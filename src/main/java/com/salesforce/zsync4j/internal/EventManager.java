/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.internal.util.HttpClient.TransferListener;

/**
 * Maintains state relevant for events, and constructs and throws externally published events as
 * necessary.
 *
 * @author bstclair
 */
public interface EventManager extends TransferListener {

  void outputFileResolved(Path outputFile);

  void zsyncStarted(URI requestedZsyncUri, Options options);

  void controlFileProcessingStarted(URI controlFileUri);

  void controlFileProcessingComplete(ControlFile controlFile);

  void inputFileProcessingStarted(Path inputFile);

  void inputFileProcessingComplete(Path inputFile);

  void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests);

  void remoteFileProcessingComplete();

  void blocksRequestStarted(List<Range> blocks);

  void blocksRequestComplete(List<Range> blocks);

  void blockProcessingStarted(Range block);

  void blockProcessingComplete(Range block);

  void bytesWritten(Path file, long bytes);

  void sha1CalculationStarted(Path file);

  void sha1CalculationComplete(String sha1);

  void moveTempFileStarted(Path tempFile, Path targetFile);

  void moveTempFileComplete();

  void zsyncFailed(Exception exception);

  void zsyncComplete();
}
