package com.salesforce.zsync4j.internal.util;

public interface ProgressMonitor {

  public static final ProgressMonitor DEFAULT = new ProgressMonitor() {

    @Override
    public void begin(long size) {}

    @Override
    public void progress(int bytesTransferred) {}

    @Override
    public void done() {}
  };

  void begin(long size);

  void progress(int bytesTransferred);

  void done();

}
