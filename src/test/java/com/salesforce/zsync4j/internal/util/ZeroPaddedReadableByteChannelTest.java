package com.salesforce.zsync4j.internal.util;

import static java.nio.channels.Channels.newChannel;
import static java.util.Arrays.copyOf;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import org.junit.Test;

/**
 * Tests various cases of {@link ZeroPaddedReadableByteChannel}
 *
 * @author bbusjaeger
 */
public class ZeroPaddedReadableByteChannelTest {

  /**
   * Test that padded channel reads regular bytes straight from the underlying channel
   */
  @Test
  public void testReadUnderlying() throws IOException {
    final byte[] b = new byte[] {1, 1, 1, 1, 1};
    final ReadableByteChannel channel = create(b, 2);
    final ByteBuffer buffer = ByteBuffer.allocate(5);

    assertEquals(buffer.capacity(), channel.read(buffer));
    assertArrayEquals(b, buffer.array());
  }

  /**
   * Tests that all padded zeros can be read at once upon exhausting underlying stream.
   */
  @Test
  public void testReadAllZeros() throws IOException {
    final byte[] b = new byte[] {1, 1, 1, 1, 1};
    final int zeros = 2;
    final ReadableByteChannel channel = create(b, zeros);
    final ByteBuffer buffer = ByteBuffer.allocate(7);

    assertEquals(b.length, channel.read(buffer));
    assertEquals(zeros, channel.read(buffer));
    assertArrayEquals(copyOf(b, buffer.capacity()), buffer.array());
    buffer.flip();
    assertEquals(-1, channel.read(buffer));
  }

  /**
   * Tests that the padded channel keeps sufficient state to read remaining zeros on subsequent
   * reads if buffer limit is exhausted.
   */
  @Test
  public void testReadSomeZeros() throws IOException {
    final byte[] b = new byte[] {1, 1, 1, 1, 1};
    final int zeros = 2;
    final ReadableByteChannel channel = create(b, zeros);
    final ByteBuffer buffer = ByteBuffer.allocate(7);

    buffer.limit(buffer.capacity() - 1);
    assertEquals(b.length, channel.read(buffer));
    assertEquals(zeros - 1, channel.read(buffer));
    assertArrayEquals(copyOf(b, buffer.capacity()), buffer.array());
    buffer.limit(buffer.capacity());
    assertEquals(1, channel.read(buffer));
    assertArrayEquals(copyOf(b, buffer.capacity()), buffer.array());
    buffer.flip();
    assertEquals(-1, channel.read(buffer));
  }

  /**
   * Tests that channel padded with zero zeros behaves just like underlying channel.
   */
  @Test
  public void testZeroZeros() throws IOException {
    final byte[] b = new byte[] {1, 1, 1, 1, 1};
    final ReadableByteChannel channel = create(b, 0);
    final ByteBuffer buffer = ByteBuffer.allocate(5);

    assertEquals(b.length, channel.read(buffer));
    assertArrayEquals(copyOf(b, buffer.capacity()), buffer.array());
    buffer.flip();
    assertEquals(-1, channel.read(buffer));
  }

  /**
   * Tests that the constructor throws an illegal argument exception if a negative number of zeros
   * is specified
   */
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNegativeZeros() {
    create(new byte[0], -1);
  }

  /**
   * Tests that the constructor throws an illegal argument exception if null is passed for the
   * underlying channel
   */
  @SuppressWarnings("resource")
  @Test(expected = IllegalArgumentException.class)
  public void testConstructorNullChannel() {
    new ZeroPaddedReadableByteChannel(null, 1);
  }

  /**
   * Tests that {@link ZeroPaddedReadableByteChannel#isOpen()} and
   * {@link ZeroPaddedReadableByteChannel#close()} exhibit desired behavior.
   */
  @Test
  public void testOpenClose() throws IOException {
    final ReadableByteChannel channel = create(new byte[0], 0);
    assertTrue(channel.isOpen());;
    channel.close();
    assertFalse(channel.isOpen());
  }

  /**
   * Test factor method: creates a padded channel off of a byte array with the given number of
   * zeros.
   * 
   * @param b byte array to construct underlying channel from
   * @param num number of zeros to bad with
   * @return padded channel
   */
  private static ReadableByteChannel create(byte[] b, int num) {
    return new ZeroPaddedReadableByteChannel(newChannel(new ByteArrayInputStream(b)), num);
  }
}
