package com.salesforce.zsync4j.internal.util;

import static com.google.common.collect.ImmutableList.of;
import static com.squareup.okhttp.Protocol.HTTP_1_1;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;

import org.junit.Test;

import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Closed;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Started;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Transferred;
import com.salesforce.zsync4j.internal.util.ObservableInputStream.ObservableResourceInputStream;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

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
    final Response response =
        new Response.Builder().request(new Request.Builder().url(uri.toURL()).build()).protocol(HTTP_1_1).code(200)
            .build();
    InputStream in =
        new ObservableResourceInputStream<Response>(new ByteArrayInputStream(new byte[0]), listener, response, 1l);
    in.close();
    assertEquals(of(new Started(uri, 1l), Closed.INSTANCE), listener.getEventLog());
  }

}
