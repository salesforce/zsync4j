package com.salesforce.zsync4j.internal.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * An input stream wrapper that lets you observe some things about bytes read from the wrapped {@link InputStream}.
 *
 * @author bstclair
 */
public class ObservableInputStream extends FilterInputStream {

  /**
   * Notified when interesting things are observed on the wrapped {@link InputStream}.
   *
   * @author bstclair
   */
  public static interface Observer {
    void bytesRead(long bytesRead, long totalBytesRead);

    void done(long totalBytesRead);
  }

  private final Observer observer;
  private long totalBytesRead;

  public ObservableInputStream(InputStream in, Observer observer) {
    super(in);
    this.observer = observer;
  }

  @Override
  public int read() throws IOException {
    final int i = super.read();
    if (i >= 0) {
      this.observer.bytesRead(1, ++this.totalBytesRead);
    }
    return i;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int i = super.read(b, off, len);
    if (i >= 0) {
      this.observer.bytesRead(i, (this.totalBytesRead += i));
    }
    return i;
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.observer.done(this.totalBytesRead);
  }
}
