package com.salesforce.zsync4j.internal.util;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

public class WritableMessageDigestTest {

  private final WritableMessageDigest writeableMessageDigest;

  public WritableMessageDigestTest() throws NoSuchAlgorithmException {
    this.writeableMessageDigest = new WritableMessageDigest(MessageDigest.getInstance("SHA-1"));
  }

  /**
   * asserts that digest is always open
   */
  @Test
  public void testOpenClose() throws IOException {
    assertTrue(this.writeableMessageDigest.isOpen());
    this.writeableMessageDigest.close();
    assertTrue(this.writeableMessageDigest.isOpen());
  }

  /**
   * asserts that write has same effect as updating the digest directly
   */
  @Test
  public void testWrite() throws IOException {
    final byte[] input = new byte[] {0, 1, 2, 3, 4, 5, 6, 7, 8, 9};
    final byte[] expected = this.writeableMessageDigest.getMessageDigest().digest(input);

    final ByteBuffer buf = ByteBuffer.wrap(input);
    assertEquals(buf.capacity(), this.writeableMessageDigest.write(buf));
    final byte[] actual = this.writeableMessageDigest.getMessageDigest().digest();

    assertArrayEquals(expected, actual);
  }
}
