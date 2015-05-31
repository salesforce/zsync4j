package com.salesforce.zsync4j;

import java.net.URI;

public interface HttpTransferObserver {
  void transferInitiated(URI uri, long contentLength);

  void bytesTransferred(long bytes);

  void transferFinished();
}
