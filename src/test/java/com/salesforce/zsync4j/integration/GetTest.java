/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

import com.salesforce.zsync4j.Zsync;
import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.ZsyncResults;
import com.squareup.okhttp.OkHttpClient;

/**
 * Describe your class here.
 *
 * @author bstclair
 */
public class GetTest extends BaseJettyTest {

  private static final String REPO_ROOT = "/.m2/repository/";

  @Test
  public void testWithOneInputFile() throws Exception {
    // Arrange
    URL oldGuava = this.getClass().getResource(REPO_ROOT + "com/google/guava/guava/15.0/guava-15.0.jar");
    URI uri = new URI(super.makeUrl("content/repositories/public/com/google/guava/guava/18.0/guava-18.0.jar.zsync"));
    Path outputPath = super.createTempFile(".jar");
    Options options = new Options().addInputFile(Paths.get(oldGuava.toURI())).setOutputFile(outputPath);

    // Act
    ZsyncResults results = new Zsync(new OkHttpClient()).zsync(uri, options);

    // Assert
    assertEquals("results has wrong output file path", outputPath, results.getOutputFile());
  }

  @Test
  @Ignore
  public void testWithTwoInputFiles() throws Exception {
    fail("Not implemented");
  }

  @Test
  @Ignore
  public void testWithZeroInputFiles() throws Exception {
    fail("Not implemented");
  }

  @Test
  @Ignore
  public void expectedExceptionIsThrownForMissingZsyncControlFile() {
    fail("Not implemented");
  }

  @Test
  @Ignore
  public void expectedExceptionIsThrownForFailedChecksumValidation() {
    fail("Not implemented");
  }

  @Test
  @Ignore
  public void expectedExceptionIsThrownForMissingRemoteFile() {
    fail("Not implemented");
  }
}
