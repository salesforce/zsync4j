package com.salesforce.zsync4j;

import java.nio.file.Path;

public interface ZsyncResults {

  Path getOutputFile();

  String getSha1();

  ZsyncStats getStats();

  Path getDownloadedControlFile();
}
