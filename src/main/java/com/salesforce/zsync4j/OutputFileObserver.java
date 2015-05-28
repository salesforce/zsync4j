package com.salesforce.zsync4j;

import java.nio.file.Path;

public interface OutputFileObserver {
  void pathAvailable(Path path);

  void contentLengthAvailable(long contentLength);

  void bytesWritten(long bytes);

  void done();

  void failed(Exception exception);
}
