/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.http.ContentRange;

/**
 * Observes events over the course of a single zsync invocation.
 *
 * @author bstclair
 */
public class ZsyncObserver {

  public void zsyncStarted(URI requestedZsyncUri, Options options) {}

  public void controlFileDownloadingInitiated(URI uri) {}

  public void controlFileDownloadingStarted(URI uri, long length) {}

  public void controlFileDownloadingComplete() {}

  public void controlFileReadingStarted(Path path, long length) {}

  public void controlFileReadingComplete() {}

  public void outputFileWritingStarted(Path outputFile, long length) {}

  public void outputFileWritingCompleted() {}

  public void inputFileReadingStarted(Path inputFile, long length) {}

  public void inputFileReadingComplete() {}

  public void remoteFileDownloadingInitiated(URI uri, List<ContentRange> ranges) {}

  public void remoteFileDownloadingStarted(URI uri, long length) {}

  public void remoteFileRangeReceived(ContentRange range) {}

  public void remoteFileDownloadingComplete() {}

  public void bytesRead(long bytes) {}

  public void bytesDownloaded(long bytes) {}

  public void bytesWritten(long bytes) {}

  public void zsyncFailed(Exception exception) {}

  public void zsyncComplete() {}
}
