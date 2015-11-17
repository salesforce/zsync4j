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

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.Test;

import com.salesforce.zsync.internal.util.WritableMessageDigest;

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
