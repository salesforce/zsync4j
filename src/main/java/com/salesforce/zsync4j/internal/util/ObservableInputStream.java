package com.salesforce.zsync4j.internal.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.salesforce.zsync4j.internal.util.TransferListener.ResourceTransferListener;

/**
 * An input stream wrapper that lets you observe some things about bytes read from the wrapped
 * {@link InputStream}.
 *
 * @author bstclair
 */
public class ObservableInputStream extends FilterInputStream {

  private final TransferListener observer;

  public ObservableInputStream(InputStream in, TransferListener observer) {
    super(in);
    this.observer = observer;
  }

  @Override
  public int read() throws IOException {
    final int i = super.read();
    if (i >= 0) {
      this.observer.transferred(1);
    }
    return i;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int i = super.read(b, off, len);
    if (i >= 0) {
      this.observer.transferred(i);
    }
    return i;
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      this.observer.close();
    }
  }

  /**
   * An input stream wrapper that lets you observe bytes of a resource with up-front known size.
   *
   * @author bbusjaeger
   */
  public static class ObservableResourceInputStream<T> extends ObservableInputStream {

    public ObservableResourceInputStream(InputStream in, ResourceTransferListener<T> observer, T resource, long size) {
      super(in, observer);
      observer.start(resource, size);
    }
  }

}