package com.salesforce.zsync4j.internal;

import java.io.IOException;

import com.salesforce.zsync4j.internal.util.HttpClient.PartialResponseBodyTransferListener;
import com.salesforce.zsync4j.internal.util.Range;

public class ZsyncObserverToPartialResponseBodyTransferListenerAdapter implements PartialResponseBodyTransferListener {

  private final ZsyncObserver delegate;

  public ZsyncObserverToPartialResponseBodyTransferListenerAdapter(ZsyncObserver delegate) {
    this.delegate = delegate;
  }

  @Override
  public void rangeRequestStarted(Iterable<? extends Range> ranges) {}

  @Override
  public void rangeRequestComplete(Iterable<? extends Range> ranges) {}

  @Override
  public void rangeProcessingStarted(Range range) {}

  @Override
  public void rangeProcessingComplete(Range range) {}

  @Override
  public void transferStarted(String uri, long totalBytes) {}

  @Override
  public void transferProgressed(int bytes) {
    this.delegate.bytesDownloaded(bytes);
  }

  @Override
  public void transferFailed(IOException exception) {}

  @Override
  public void transferComplete() {}

  @Override
  public void transferClosed() {}
}
