package com.salesforce.zsync4j.internal.util;

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

public class ProgressMonitorInputStream extends FilterInputStream {

  private final ProgressMonitor monitor;
  private int read;

  protected ProgressMonitorInputStream(InputStream in, long size, ProgressMonitor monitor) {
    super(in);
    this.monitor = monitor;

    monitor.begin(size);
    read = 0;
  }

  @Override
  public int read() throws IOException {
    final int i = super.read();
    if (i >= 0)
      monitor.progress(++read);
    return i;
  }

  @Override
  public int read(byte[] b) throws IOException {
    final int i = super.read(b);
    if (i >= 0)
      monitor.progress(read += i);
    return i;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int i = super.read(b, off, len);
    if (i >= 0)
      monitor.progress(read += i);
    return i;
  }

  @Override
  public void close() throws IOException {
    super.close();
    monitor.done();
  }

}
