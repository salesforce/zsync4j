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

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;

import org.junit.Test;

import com.salesforce.zsync.internal.util.ObservableRedableByteChannel;
import com.salesforce.zsync.internal.util.EventLogHttpTransferListener.Closed;
import com.salesforce.zsync.internal.util.EventLogHttpTransferListener.Transferred;

public class ObservableRedableByteChannelTest {

  /**
   * asserts reads are reported
   */
  @Test
  public void testRead() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(new byte[3]));
    @SuppressWarnings("resource")
    ReadableByteChannel in = new ObservableRedableByteChannel(channel, listener);
    ByteBuffer dst = ByteBuffer.allocate(4);
    dst.limit(1);
    assertEquals(1, in.read(dst));
    dst.limit(4);
    assertEquals(2, in.read(dst));
    assertEquals(-1, in.read(dst));
    assertEquals(of(new Transferred(1), new Transferred(2)), listener.getEventLog());
  }

  /**
   * asserts close is reported
   */
  @Test
  public void testClose() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    ReadableByteChannel channel = Channels.newChannel(new ByteArrayInputStream(new byte[10]));
    ReadableByteChannel in = new ObservableRedableByteChannel(channel, listener);
    in.close();
    assertFalse(in.isOpen());
    assertEquals(of(Closed.INSTANCE), listener.getEventLog());
  }

  /**
   * asserts that exception is re-thrown from close and that listener is still closed
   */
  @Test
  public void testCloseFailed() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    ReadableByteChannel in = mock(ReadableByteChannel.class);
    doThrow(new IOException("test")).when(in).close();
    ReadableByteChannel oin = new ObservableRedableByteChannel(in, listener);
    try {
      oin.close();
      fail("expected exception not thrown");
    } catch (IOException e) {
      assertEquals("test", e.getMessage());
    }
    assertEquals(of(Closed.INSTANCE), listener.getEventLog());
  }

}
