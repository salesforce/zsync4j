package com.salesforce.zsync4j.internal.util;

import java.io.Closeable;
import java.io.IOException;

/**
 * Notified when transfer is observed on some wrapped resource
 *
 * @author bstclair
 */
public interface TransferListener extends Closeable {

  /**
   * Indicates that the given number of bytes have been transferred
   * 
   * @param bytes number of bytes transferred
   */
  void transferred(long bytes);

  /**
   * Indicates that the transfer has finished. The transfer may have completed successfully or not.
   */
  @Override
  void close() throws IOException;

  /**
   * 
   * Notified when resource transfer is started
   *
   * @author bbusjaeger
   * @param <T> Type of resource being transferred
   */
  public static interface ResourceTransferListener<T> extends TransferListener {

    void start(T resource, long length);

  }

}
