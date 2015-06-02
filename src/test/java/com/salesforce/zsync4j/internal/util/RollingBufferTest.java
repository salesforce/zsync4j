package com.salesforce.zsync4j.internal.util;

import static java.nio.channels.Channels.newChannel;
import static java.util.Arrays.fill;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.channels.WritableByteChannel;

import org.junit.Test;

public class RollingBufferTest {

  /**
   * Tests that constructor throws IAE if channel is null
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorChannelNull() throws IOException {
    new RollingBuffer(null, 1, 2);
  }

  /**
   * Tests that constructor throws IAE if channel has insufficient bytes to initialize window
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorChannelTooSmall() throws IOException {
    new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[0])), 1, 2);
  }

  /**
   * Tests that constructor throws IAE if window size is negative
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNegativeWindowSize() throws IOException {
    final ReadableByteChannel c = mock(ReadableByteChannel.class);
    new RollingBuffer(c, -1, 1);
  }

  /**
   * Tests that constructor throws IAE if buffer size is negative
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNegativeBufferSize() throws IOException {
    final ReadableByteChannel c = mock(ReadableByteChannel.class);
    new RollingBuffer(c, 1, -1);
  }

  /**
   * Tests that constructor throws IAE if buffer is too small relative to window
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorBufferSizeTooSmall() throws IOException {
    final ReadableByteChannel c = mock(ReadableByteChannel.class);
    new RollingBuffer(c, 2048, 2048);
  }

  /**
   * Tests that constructor throws IAE if channel has fewer bytes than required by window size
   */
  @Test(expected = IllegalArgumentException.class)
  public void testChannelTooSmall() throws IOException {
    final ReadableByteChannel c = Channels.newChannel(new ByteArrayInputStream(new byte[1024]));
    new RollingBuffer(c, 2048, 2048);
  }

  /**
   * Tests that rolling buffer cannot move backwards (rejects negative argument to advance method)
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAdvanceBackwards() throws IOException {
    final ReadableByteChannel c = Channels.newChannel(new ByteArrayInputStream(new byte[1024]));
    final RollingBuffer b = new RollingBuffer(c, 256, 512);
    b.advance(-1);
  }

  /**
   * Tests that rolling buffer cannot advance beyond current window
   */
  @Test(expected = IllegalArgumentException.class)
  public void testAdvanceBeyondWindow() throws IOException {
    final ReadableByteChannel c = Channels.newChannel(new ByteArrayInputStream(new byte[1024]));
    final RollingBuffer b = new RollingBuffer(c, 256, 512);
    b.advance(257);
  }

  /**
   * Tests that rolling buffer cannot advance beyond current window
   */
  @Test
  public void testAdvanceOne() throws IOException {
    final byte[] data = new byte[1024];
    fill(data, 256, 511, (byte) 1);
    final ReadableByteChannel c = Channels.newChannel(new ByteArrayInputStream(data));
    final RollingBuffer b = new RollingBuffer(c, 256, 512);
    assertTrue(b.advance(1));
    assertEquals((byte) 0, b.get(254));
    assertEquals((byte) 1, b.get(255));
  }

  /**
   * Tests that the buffer is refilled as expected if the window is advanced beyond the end.
   */
  @Test
  public void testAdvanceBuffer() throws IOException {
    final byte[] data = new byte[16];
    fill(data, 4, 8, (byte) 1);
    fill(data, 8, 12, (byte) 2);
    fill(data, 12, 16, (byte) 3);

    final ReadableByteChannel c = newChannel(new ByteArrayInputStream(data));
    final RollingBuffer b = new RollingBuffer(c, 4, 8);

    assertArrayEquals(new byte[] {0, 0, 0, 0}, read(b));

    assertTrue(b.advance(4));
    assertArrayEquals(new byte[] {1, 1, 1, 1}, read(b));

    assertTrue(b.advance(4));
    assertArrayEquals(new byte[] {2, 2, 2, 2}, read(b));

    assertTrue(b.advance(4));
    assertArrayEquals(new byte[] {3, 3, 3, 3}, read(b));

    assertFalse(b.advance(4));
  }

  /**
   * Tests that buffer cannot be advanced if insufficient bytes are available.
   */
  @Test
  public void testAdvanceInsufficientBytes() throws IOException {
    final byte[] data = new byte[8];
    fill(data, 2, 4, (byte) 1);
    fill(data, 4, 6, (byte) 2);
    fill(data, 6, 8, (byte) 3);

    final ReadableByteChannel c = newChannel(new ByteArrayInputStream(data));
    final RollingBuffer b = new RollingBuffer(c, 2, 6);

    assertArrayEquals(new byte[] {0, 0}, read(b));

    assertTrue(b.advance(2));
    assertArrayEquals(new byte[] {1, 1}, read(b));

    assertTrue(b.advance(2));
    assertArrayEquals(new byte[] {2, 2}, read(b));

    assertTrue(b.advance(2));
    assertArrayEquals(new byte[] {3, 3}, read(b));

    assertFalse(b.advance(2));
  }

  /**
   * Tests that the buffer returns the window size as its length
   */
  @Test
  public void testLength() throws IOException {
    final RollingBuffer b = new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[2])), 1, 2);
    assertEquals(1, b.length());
  }

  /**
   * Tests that the get method throws an IndexOutOfBoundsException if the index is negative
   */
  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetIndexNegative() throws IOException {
    final RollingBuffer b = new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[2])), 1, 2);
    b.get(-1);
  }

  /**
   * Tests that the get method throws an IndexOutOfBoundsException if the index is beyond the length
   */
  @Test(expected = IndexOutOfBoundsException.class)
  public void testGetIndexBeyondLength() throws IOException {
    final RollingBuffer b = new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[2])), 1, 2);
    b.get(2);
  }

  /**
   * Tests that the write method throws an IndexOutOfBoundsException if the offset is negative
   */
  @Test(expected = IndexOutOfBoundsException.class)
  public void testWriteOffsetNegative() throws IOException {
    final RollingBuffer b = new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[2])), 1, 2);
    b.write(Channels.newChannel(new ByteArrayOutputStream()), -1, 1);
  }

  /**
   * Tests that the write method throws an IndexOutOfBoundsException if the offset is beyond length
   */
  @Test(expected = IndexOutOfBoundsException.class)
  public void testWriteOffsetBeyondLength() throws IOException {
    final RollingBuffer b = new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[2])), 1, 2);
    b.write(Channels.newChannel(new ByteArrayOutputStream()), 1, 1);
  }

  /**
   * Tests that the write method throws an IndexOutOfBoundsException if given the offset, the length
   * extends beyond the window size
   */
  @Test(expected = IndexOutOfBoundsException.class)
  public void testWriteLengthBeyondWindow() throws IOException {
    final RollingBuffer b = new RollingBuffer(newChannel(new ByteArrayInputStream(new byte[4])), 1, 2);
    b.write(Channels.newChannel(new ByteArrayOutputStream()), 0, 2);
  }

  private static byte[] read(ReadableByteBuffer b) throws IOException {
    final ByteArrayOutputStream bos = new ByteArrayOutputStream();
    final WritableByteChannel o = Channels.newChannel(bos);
    b.write(o);
    return bos.toByteArray();
  }
}
