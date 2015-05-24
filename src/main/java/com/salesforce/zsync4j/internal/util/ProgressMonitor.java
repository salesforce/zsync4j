package com.salesforce.zsync4j.internal.util;

public interface ProgressMonitor {

  void begin(long size);

  void progress(int completed);

  void done();

}
