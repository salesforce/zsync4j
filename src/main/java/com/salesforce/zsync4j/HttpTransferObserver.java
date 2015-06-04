package com.salesforce.zsync4j;

import java.net.URI;

/**
 * Observes HTTP activity over a connection to a specified URI
 *
 * @author bstclair
 */
public interface HttpTransferObserver {
  void transferInitiated(URI uri, long contentLength);

  void bytesTransferred(long bytes);

  void transferFinished();
}
