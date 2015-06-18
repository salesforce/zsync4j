package com.salesforce.zsync4j.internal.util;

import static java.util.Arrays.copyOf;
import static java.util.Arrays.copyOfRange;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

import org.junit.Test;

public class SplitInputStreamTest {

  /**
   * Tests that the read method correctly terminates the stream after the boundary has been read and
   * that reading the next input stream resumes after the boundary.
   */
  @Test
  public void testRead() throws IOException {
    final byte[] data = new byte[] {'a', 'b', '\r', '\n', '\r', '\n', 'c', 'd'};
    @SuppressWarnings("resource")
    final SplitInputStream sin = new SplitInputStream(new ByteArrayInputStream(data), new byte[] {'\r', '\n'});

    assertEquals('a', sin.read());
    assertEquals('b', sin.read());
    assertEquals('\r', sin.read());
    assertEquals('\n', sin.read());
    assertEquals(-1, sin.read());
    assertEquals(-1, sin.read());

    final InputStream in = sin.next();
    assertEquals('\r', in.read());
    assertEquals('\n', in.read());
    assertEquals('c', in.read());
    assertEquals('d', in.read());
    assertEquals(-1, in.read());
  }

  /**
   * Tests that read returns -1 if the end of the stream is reached before a boundary is found and
   * that next throws an ISE in that case.
   */
  @Test
  public void testReadNoBoundary() throws IOException {
    final byte[] data = new byte[] {'a', 'b', '\r'};
    @SuppressWarnings("resource")
    final SplitInputStream sin = new SplitInputStream(new ByteArrayInputStream(data), new byte[] {'\r', '\n'});
    assertEquals('a', sin.read());
    assertEquals('b', sin.read());
    assertEquals('\r', sin.read());
    assertEquals(-1, sin.read());

    try {
      sin.next();
    } catch (IllegalStateException e) {
      assertEquals("boundary not reached", e.getMessage());
    }
  }

  /**
   * Asserts that reading a byte array across the boundary stops at the boundary as expected
   */
  @Test
  public void testReadArray() throws IOException {
    final byte[] data =
        new byte[] {'a', 'b', 'c', '\r', 'd', 'e', 'f', '\n', 'g', 'h', '\r', '\n', 'i', 'j', 'k', '\r', '\n', 'l', 'm'};
    @SuppressWarnings("resource")
    final SplitInputStream sin = new SplitInputStream(new ByteArrayInputStream(data), new byte[] {'\r', '\n'});
    final byte[] b = new byte[11];
    assertEquals(b.length, sin.read(b));
    assertArrayEquals(copyOf(data, b.length), b);
    assertEquals(1, sin.read(b));
    assertEquals('\n', b[0]);
    assertEquals(-1, sin.read(b));

    InputStream in = sin.next();
    Arrays.fill(b, (byte) 0);
    assertEquals(7, in.read(b));
    assertArrayEquals(copyOfRange(data, 12, 19), copyOf(b, 7));
    assertEquals(-1, in.read());
  }

  @Test
  public void testReadEndBeforeBoundary() throws IOException {
    final byte[] data = new byte[0];
    @SuppressWarnings("resource")
    final SplitInputStream sin = new SplitInputStream(new ByteArrayInputStream(data), new byte[] {'\r', '\n'});
    final byte[] b = new byte[1];
    assertEquals(-1, sin.read(b));
    try {
      sin.next();
    } catch (IllegalStateException e) {
      assertEquals("boundary not reached", e.getMessage());
    }
  }
}
