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
package com.salesforce.zsync.internal.util;

import com.salesforce.zsync.internal.util.EventLogHttpTransferListener.Closed;
import com.salesforce.zsync.internal.util.EventLogHttpTransferListener.Started;
import com.salesforce.zsync.internal.util.EventLogHttpTransferListener.Transferred;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import static com.google.common.collect.ImmutableList.of;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class ObservableInputStreamTest {

  /**
   * asserts reads are reported
   */
  @Test
  public void testRead() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    @SuppressWarnings("resource")
    InputStream in = new ObservableInputStream(new ByteArrayInputStream(new byte[10]), listener);
    in.read();
    in.read(new byte[2]);
    assertEquals(of(new Transferred(1), new Transferred(2)), listener.getEventLog());
  }

  /**
   * asserts reads are not reported if the stream is exhausted
   */
  @Test
  public void testNoRead() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    @SuppressWarnings("resource")
    InputStream in = new ObservableInputStream(new ByteArrayInputStream(new byte[0]), listener);
    in.read();
    in.read(new byte[2]);
    assertTrue(listener.getEventLog().isEmpty());
  }

  /**
   * asserts close is reported
   */
  @Test
  public void testClose() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    InputStream in = new ObservableInputStream(new ByteArrayInputStream(new byte[0]), listener);
    in.close();
    assertEquals(of(Closed.INSTANCE), listener.getEventLog());
  }

  /**
   * asserts that exception is re-thrown from close and that listener is still closed
   */
  @Test
  public void testCloseFailed() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    InputStream in = mock(InputStream.class);
    doThrow(new IOException("test")).when(in).close();
    InputStream oin = new ObservableInputStream(in, listener);
    try {
      oin.close();
      fail("expected exception not thrown");
    } catch (IOException e) {
      assertEquals("test", e.getMessage());
    }
    assertEquals(of(Closed.INSTANCE), listener.getEventLog());
  }

  @Test
  public void testStarted() throws IOException {
    EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    final URI uri = URI.create("http://host/path");

    HttpResponse<byte[]> mockResponse = mock(HttpResponse.class);
    HttpRequest request = HttpRequest.newBuilder(uri).build();
    when(mockResponse.request()).thenReturn(request);

    InputStream in =
        new ObservableInputStream.ObservableResourceInputStream<HttpResponse<byte[]>>(new ByteArrayInputStream(new byte[0]), listener, mockResponse, 1l);
    in.close();
    assertEquals(of(new Started(uri, 1l), Closed.INSTANCE), listener.getEventLog());
  }

}
