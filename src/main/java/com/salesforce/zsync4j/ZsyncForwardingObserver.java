/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j;

import static com.google.common.collect.ImmutableList.copyOf;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.http.ContentRange;


/**
 * A {@link ZsyncObserver} that forwards observed events to a configurable list of additional zsync
 * observers.
 *
 * @author bstclair
 */
public class ZsyncForwardingObserver extends ZsyncObserver {

  private List<ZsyncObserver> observers;

  public ZsyncForwardingObserver(ZsyncObserver... targets) {
    this(targets == null ? ImmutableList.<ZsyncObserver>of() : ImmutableList.copyOf(targets));
  }

  public ZsyncForwardingObserver(Iterable<? extends ZsyncObserver> observers) {
    Preconditions.checkNotNull(observers);
    this.observers = copyOf(observers);
  }

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {
    for (ZsyncObserver observer : this.observers) {
      observer.zsyncStarted(requestedZsyncUri, options);
    }
  }

  @Override
  public void controlFileDownloadingInitiated(URI uri) {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileDownloadingInitiated(uri);
    }
  }

  @Override
  public void controlFileDownloadingStarted(URI uri, long length) {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileDownloadingStarted(uri, length);
    }
  }

  @Override
  public void controlFileDownloadingComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileDownloadingComplete();
    }
  }

  @Override
  public void controlFileReadingStarted(Path path, long length) {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileReadingStarted(path, length);
    }
  }

  @Override
  public void controlFileReadingComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileReadingComplete();
    }
  }

  @Override
  public void inputFileReadingStarted(Path inputFile, long length) {
    for (ZsyncObserver observer : this.observers) {
      observer.inputFileReadingStarted(inputFile, length);
    }
  }

  @Override
  public void inputFileReadingComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.inputFileReadingComplete();
    }
  }

  @Override
  public void remoteFileDownloadingInitiated(URI uri, List<ContentRange> ranges) {
    for (ZsyncObserver observer : this.observers) {
      observer.remoteFileDownloadingInitiated(uri, ranges);
    }
  }

  @Override
  public void remoteFileDownloadingStarted(URI uri, long length) {
    for (ZsyncObserver observer : this.observers) {
      observer.remoteFileDownloadingStarted(uri, length);
    }
  }

  @Override
  public void remoteFileRangeReceived(ContentRange range) {
    for (ZsyncObserver observer : this.observers) {
      observer.remoteFileRangeReceived(range);
    }
  }

  @Override
  public void remoteFileDownloadingComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.remoteFileDownloadingComplete();
    }
  }

  @Override
  public void outputFileWritingStarted(Path outputFile, long length) {
    for (ZsyncObserver observer : this.observers) {
      observer.outputFileWritingStarted(outputFile, length);
    }
  }

  @Override
  public void outputFileWritingCompleted() {
    for (ZsyncObserver observer : this.observers) {
      observer.outputFileWritingCompleted();
    }
  }

  @Override
  public void bytesRead(long bytes) {
    for (ZsyncObserver observer : this.observers) {
      observer.bytesRead(bytes);
    }
  }

  @Override
  public void bytesWritten(long bytes) {
    for (ZsyncObserver observer : this.observers) {
      observer.bytesWritten(bytes);
    }
  }

  @Override
  public void bytesDownloaded(long bytes) {
    for (ZsyncObserver observer : this.observers) {
      observer.bytesDownloaded(bytes);
    }
  }

  @Override
  public void zsyncFailed(Exception exception) {
    for (ZsyncObserver observer : this.observers) {
      observer.zsyncFailed(exception);
    }
  }

  @Override
  public void zsyncComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.zsyncComplete();
    }
  }
}
