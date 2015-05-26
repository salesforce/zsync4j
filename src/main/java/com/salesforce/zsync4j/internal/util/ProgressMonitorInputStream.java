package com.salesforce.zsync4j.internal.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressMonitorInputStream extends FilterInputStream {

  private final ProgressMonitor monitor;
  private int read;

  public ProgressMonitorInputStream(InputStream in, long size, ProgressMonitor monitor) {
    super(in);
    this.monitor = monitor;

    monitor.begin(size);
    this.read = 0;
  }

  @Override
  public int read() throws IOException {
    final int i = super.read();
    if (i >= 0) {
      this.monitor.progress(++this.read);
    }
    return i;
  }

  @Override
  public int read(byte[] b) throws IOException {
    final int i = super.read(b);
    if (i >= 0) {
      this.monitor.progress(this.read += i);
    }
    return i;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int i = super.read(b, off, len);
    if (i >= 0) {
      this.monitor.progress(this.read += i);
    }
    return i;
  }

  @Override
  public void close() throws IOException {
    super.close();
    this.monitor.done();
  }

}
