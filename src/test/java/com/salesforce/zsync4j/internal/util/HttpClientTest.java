package com.salesforce.zsync4j.internal.util;

import static com.squareup.okhttp.Protocol.HTTP_1_1;
import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import okio.BufferedSource;

import org.junit.Test;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.salesforce.zsync4j.internal.util.EventLogTransferListener.Closed;
import com.salesforce.zsync4j.internal.util.EventLogTransferListener.Completed;
import com.salesforce.zsync4j.internal.util.EventLogTransferListener.Event;
import com.salesforce.zsync4j.internal.util.EventLogTransferListener.Progressed;
import com.salesforce.zsync4j.internal.util.EventLogTransferListener.Started;
import com.salesforce.zsync4j.internal.util.HttpClient.PartialResponseBodyTransferListener;
import com.salesforce.zsync4j.internal.util.HttpClient.RangeReceiver;
import com.squareup.okhttp.Call;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Protocol;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public class HttpClientTest {

  @Test
  public void exceptionThrownFromConstructorForNullHttpClient() {
    // Act
    try {
      new HttpClient(null);
    } catch (IllegalArgumentException exception) {

      // Assert
      assertEquals("httpClient cannot be null", exception.getMessage());
    }
  }

  @Test
  public void runtimeExceptionThrownForIoExceptionDuringHttpCommunication() throws Exception {
    // Arrange
    OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    RangeReceiver mockReceiver = mock(RangeReceiver.class);
    PartialResponseBodyTransferListener listener = mock(PartialResponseBodyTransferListener.class);
    List<Range> ranges = this.createSomeRanges(1);
    URI url = new URI("someurl");
    IOException expected = new IOException("IO");
    Call mockCall = mock(Call.class);
    when(mockCall.execute()).thenThrow(expected);
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

    // Act
    try {
      new HttpClient(mockHttpClient).partialGet(url, ranges, mockReceiver, listener);
    } catch (IOException exception) {

      // Assert
      assertEquals("IO", exception.getMessage());
    }
  }

  @Test
  public void runtimeExceptionThrownForHttpResponsesOtherThan206() throws Exception {
    // Arrange
    List<Integer> responsesToTest = Lists.newArrayList(500, 413); // Add whatever other ones we want
    OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    RangeReceiver mockReceiver = mock(RangeReceiver.class);
    PartialResponseBodyTransferListener listener = mock(PartialResponseBodyTransferListener.class);
    List<Range> ranges = this.createSomeRanges(1);
    URI url = new URI("someurl");

    for (Integer responseToTest : responsesToTest) {

      // Arrange some more
      Call mockCall = mock(Call.class);
      Response fakeResponse = this.fakeResponse(responseToTest);
      when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(fakeResponse);

      // Act
      try {
        new HttpClient(mockHttpClient).partialGet(url, ranges, mockReceiver, listener);
      } catch (IOException exception) {

        // Assert
        assertEquals("Http request for resource " + url + " returned unexpected http code: " + responseToTest,
            exception.getMessage());
      }
    }

  }

  private Response fakeResponse(int code) {
    Request fakeRequest = new Request.Builder().url("url").build();
    return new Response.Builder().protocol(Protocol.HTTP_2).request(fakeRequest).code(code).build();
  }

  @Test
  public void testTransferListener() throws IOException {
    final URI uri = URI.create("tmp");

    final byte[] data = new byte[17];
    final ResponseBody body = mock(ResponseBody.class);
    when(body.contentLength()).thenReturn((long) data.length);
    when(body.source()).thenReturn(mock(BufferedSource.class));
    final InputStream inputStream = new ByteArrayInputStream(data);
    when(body.byteStream()).thenReturn(inputStream);

    final Request request = new Request.Builder().url(uri.toString()).build();
    final Response response = new Response.Builder().protocol(HTTP_1_1).body(body).request(request).code(200).build();

    final OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    final Call mockCall = mock(Call.class);
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
    when(mockCall.execute()).thenReturn(response);

    final EventLogTransferListener listener = new EventLogTransferListener();
    final InputStream in = new HttpClient(mockHttpClient).get(uri, listener);
    final byte[] b = new byte[8];
    assertEquals(0, in.read());
    assertEquals(8, in.read(b));
    assertEquals(8, in.read(b, 0, 8));
    assertEquals(-1, in.read());
    in.close();

    final List<Event> events =
        ImmutableList.of(new Started(uri.toString(), data.length), new Progressed(1), new Progressed(8),
            new Progressed(8), Completed.INSTANCE, Closed.INSTANCE);
    assertEquals(events, listener.getEventLog());
  }

  private List<Range> createSomeRanges(int numberOfRangesToCreate) {
    List<Range> ranges = new ArrayList<>(numberOfRangesToCreate);
    int rangeStart = 0;
    int rangeSize = 10;
    for (int i = 0; i < numberOfRangesToCreate; i++) {
      rangeStart = i * rangeSize;
      int rangeEnd = rangeStart + rangeSize - 1;
      ranges.add(new Range(rangeStart, rangeEnd));
    }
    return ranges;
  }
}
