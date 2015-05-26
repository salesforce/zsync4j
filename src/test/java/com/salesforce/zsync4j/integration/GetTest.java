/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.integration;

import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Test;

import com.salesforce.zsync4j.Zsync;
import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.ZsyncListener;
import com.squareup.okhttp.OkHttpClient;

/**
 * Describe your class here.
 *
 * @author bstclair
 */
public class GetTest extends BaseJettyTest {

  public static final ZsyncListener DEFAULT_LISTENER = new ZsyncListener() {
    @Override
    public void transferStarted(URI controlFileUri, Options options) {
      System.out.println("Downloading: " + controlFileUri.toString());
    }

    @Override
    public void transferComplete(Path outputFile, String sha1, long totalBytesDownloaded,
        long totalTransferTimeInMilliseconds) {
      long kbDownloaded = totalBytesDownloaded / 1000;
      long kbWritten = 0;
      try {
        kbWritten = Files.size(outputFile) / 1000;
      } catch (IOException exception) {
        throw new RuntimeException("Failed to convert file size to KB: " + outputFile, exception);
      }

      double totalTransferTimeInSeconds = (double) totalTransferTimeInMilliseconds / 1000;
      double effectiveDownloadRate = kbWritten / totalTransferTimeInSeconds;
      System.out.format(
          "Downloaded: %s, SHA-1 is %s (Downloaded %s KB, wrote %s KB, effective download rate %.2f KB/s)\n",
          outputFile.toString(), sha1, kbDownloaded, kbWritten, effectiveDownloadRate);
    }

    @Override
    public void transferFailed(URI controlFileUri, Exception exception) {
      System.out.println(exception);
    }
  };

  private static final String REPO_ROOT = "/.m2/repository/";

  @Test
  public void testGet() throws Exception {
    // Arrange
    URL oldGuava1 = this.getClass().getResource(REPO_ROOT + "com/google/guava/guava/15.0/guava-15.0.jar");
    URI uri = new URI(super.makeUrl("content/repositories/public/com/google/guava/guava/18.0/guava-18.0.jar.zsync"));
    Path outputPath = super.createTempFile(".jar");
    Options options = new Options().addInputFile(Paths.get(oldGuava1.toURI())).setOutputFile(outputPath);

    // Act
    new Zsync(new OkHttpClient(), DEFAULT_LISTENER).zsync(uri, options);

    // Assert
    assertNotNull(outputPath);
  }
}
