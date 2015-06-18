package com.salesforce.zsync4j.internal.util;

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
