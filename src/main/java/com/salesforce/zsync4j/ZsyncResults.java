package com.salesforce.zsync4j;

import java.nio.file.Path;

public class ZsyncResults {

  private final Path outputFile;
  private final String sha1;
  private final ZsyncStats stats;

  public ZsyncResults(Path outputFile, String sha1, ZsyncStats stats) {
    this.outputFile = outputFile;
    this.sha1 = sha1;
    this.stats = stats;
  }

  public Path getOutputFile() {
    return this.outputFile;
  }

  public String getSha1() {
    return this.sha1;
  }

  public ZsyncStats getStats() {
    return this.stats;
  }
}
