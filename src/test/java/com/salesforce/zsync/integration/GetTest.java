/**
 * Copyright (c) 2015, Salesforce.com, Inc. All rights reserved.
 * Copyright (c) 2020, Bitshift (bitshifted.co), Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.zsync.integration;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Ignore;
import org.junit.Test;

import com.salesforce.zsync.Zsync;
import com.salesforce.zsync.Zsync.Options;


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
    Path result = new Zsync().zsync(uri, options);

    // Assert
    assertEquals("results has wrong output file path", outputPath, result);
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
