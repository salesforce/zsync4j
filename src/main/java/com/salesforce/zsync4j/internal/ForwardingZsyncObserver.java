/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.HashSet;
import java.util.Set;

import com.salesforce.zsync4j.Zsync.Options;


/**
 * A {@link ZsyncObserver} that forwards observed events to a configurable list of additional zsync observers.
 *
 * @author bstclair
 */
public class ForwardingZsyncObserver extends ZsyncObserver {

  private Set<ZsyncObserver> observers = new HashSet<>();

  public ForwardingZsyncObserver(ZsyncObserver... targets) {
    if (targets != null) {
      for (ZsyncObserver observer : targets) {
        this.addObserver(observer);
      }
    }
  }

  @Override
  public void bytesDownloaded(long bytes) {
    for (ZsyncObserver observer : this.observers) {
      observer.bytesDownloaded(bytes);
    }
  }

  public void addObserver(ZsyncObserver observer) {
    this.observers.add(observer);
  }

  public void removeObserver(ZsyncObserver observer) {
    this.observers.remove(observer);
  }

  @Override
  public void zsyncStarted(URI requestedZsyncUri, Options options) {
    for (ZsyncObserver observer : this.observers) {
      observer.zsyncStarted(requestedZsyncUri, options);
    }
  }

  @Override
  public void controlFileProcessingStarted(URI controlFileUri) {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileProcessingStarted(controlFileUri);
    }
  }

  @Override
  public void controlFileProcessingComplete(ControlFile controlFile) {
    for (ZsyncObserver observer : this.observers) {
      observer.controlFileProcessingComplete(controlFile);
    }
  }

  @Override
  public void inputFileProcessingStarted(Path inputFile) {
    for (ZsyncObserver observer : this.observers) {
      observer.inputFileProcessingStarted(inputFile);
    }
  }

  @Override
  public void inputFileProcessingComplete(Path inputFile) {
    for (ZsyncObserver observer : this.observers) {
      observer.inputFileProcessingComplete(inputFile);
    }
  }

  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {
    for (ZsyncObserver observer : this.observers) {
      observer.remoteFileProcessingStarted(remoteUri, expectedBytes, expectedBlocks, expectedRequests);
    }
  }

  @Override
  public void remoteFileProcessingComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.remoteFileProcessingComplete();
    }
  }

  @Override
  public void bytesWritten(Path file, long bytes) {
    for (ZsyncObserver observer : this.observers) {
      observer.bytesWritten(file, bytes);
    }
  }

  @Override
  public void sha1Calculated(String sha1) {
    for (ZsyncObserver observer : this.observers) {
      observer.sha1Calculated(sha1);
    }
  }

  @Override
  public void zsyncFailed(Exception exception) {
    for (ZsyncObserver observer : this.observers) {
      observer.zsyncFailed(exception);
    }
  }

  @Override
  public void outputFileResolved(Path outputFile) {
    for (ZsyncObserver observer : this.observers) {
      observer.outputFileResolved(outputFile);
    }
  }

  @Override
  public void zsyncComplete() {
    for (ZsyncObserver observer : this.observers) {
      observer.zsyncComplete();
    }
  }
}
