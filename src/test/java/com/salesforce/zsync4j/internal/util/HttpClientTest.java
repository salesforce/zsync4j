package com.salesforce.zsync4j.internal.util;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;

import com.google.common.collect.Lists;
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
  public void testGetWithProgress() throws IOException {
    final ResponseBody body = mock(ResponseBody.class);
    when(body.contentLength()).thenReturn(1024l);
    final Response response = new Response.Builder().body(body).code(200).build();

    final OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    final Call mockCall = mock(Call.class);
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
    when(mockCall.execute()).thenReturn(response);

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
