/**
 * Copyright (c) 2015, Salesforce.com, Inc. All rights reserved.
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
package com.salesforce.zsync.internal.util;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.salesforce.zsync.internal.util.ZsyncUtil;

public class ZsyncUtilTest {

  @Test
  public void testToHexString() {
    assertEquals("01fe", ZsyncUtil.toHexString(ByteBuffer.wrap(new byte[] {1, (byte) -2})));
  }

  @Test
  public void testToHexStringEmtpy() {
    assertEquals("", ZsyncUtil.toHexString(ByteBuffer.allocate(0)));
  }

  @Test
  public void testToInt() {
    assertEquals(65537, ZsyncUtil.toInt((short) 1, (short) 1));
  }

  @Test
  public void testToLong() {
    assertEquals(4294967297l, ZsyncUtil.toLong(1, 1));
  }

  @Test
  public void testUnsignedNegative() {
    assertEquals((short) 255, ZsyncUtil.unsigned((byte) -1));
  }

  @Test
  public void testUnsignedPositive() {
    assertEquals((short) 1, ZsyncUtil.unsigned((byte) 1));
  }

  @Test
  public void testComputeSha1() throws IOException {
    final byte[] buf = new byte[] {0, 1, 2, 3, 4, 5, 6, 7};
    ReadableByteChannel c = Channels.newChannel(new ByteArrayInputStream(buf));
    assertEquals("67423ebfa8454f19ac6f4686d6c0dc731a3ddd6b", ZsyncUtil.computeSha1(c));;
  }

  /**
   * Asserts that file path can be looked up by URI
   */
  @Test
  public void testGetPath() throws IOException {
    Path p = Files.createTempFile(null, null);
    try {
      assertEquals(p, ZsyncUtil.getPath(p.toUri()));
    } finally {
      Files.delete(p);
    }
  }

  /**
   * Asserts that relative URI throws IAE
   */
  @Test(expected = IllegalArgumentException.class)
  public void testGetPathIAE() {
    ZsyncUtil.getPath(URI.create("test"));
  }

  /**
   * Asserts that HTTP URI is not resolved to path
   */
  @Test()
  public void testGetPathNull() {
    assertNull(ZsyncUtil.getPath(URI.create("http://host/test")));
  }

}
