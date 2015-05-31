package com.salesforce.zsync4j;

import java.nio.file.Path;
import java.util.Map;

public interface ZsyncStats {

  long getTotalBytesDownloaded();

  long getBytesDownloadedForControlFile();

  long getBytesDownloadedFromRemoteTarget();

  long getOutputFileSize();

  long getTotalElapsedMilliseconds();

  Map<Path, Long> getContributedBytesByInputFile();
}
