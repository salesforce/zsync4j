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
package com.salesforce.zsync4j.internal.util;

import static com.squareup.okhttp.Protocol.HTTP_1_1;
import static java.net.HttpURLConnection.HTTP_OK;
import static java.net.HttpURLConnection.HTTP_UNAUTHORIZED;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import okio.BufferedSource;

import org.junit.Test;

import com.google.common.base.Function;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import com.salesforce.zsync4j.http.ContentRange;
import com.salesforce.zsync4j.http.Credentials;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Closed;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Event;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Initialized;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Started;
import com.salesforce.zsync4j.internal.util.EventLogHttpTransferListener.Transferred;
import com.salesforce.zsync4j.internal.util.HttpClient.HttpTransferListener;
import com.salesforce.zsync4j.internal.util.HttpClient.RangeReceiver;
import com.salesforce.zsync4j.internal.util.HttpClient.RangeTransferListener;
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

  @SuppressWarnings("unchecked")
  @Test
  public void runtimeExceptionThrownForIoExceptionDuringHttpCommunication() throws Exception {
    // Arrange
    OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    RangeReceiver mockReceiver = mock(RangeReceiver.class);
    RangeTransferListener listener = mock(RangeTransferListener.class);
    when(listener.newTransfer(any(List.class))).thenReturn(mock(HttpTransferListener.class));
    List<ContentRange> ranges = this.createSomeRanges(1);
    URI url = new URI("http://host/someurl");
    IOException expected = new IOException("IO");
    Call mockCall = mock(Call.class);
    when(mockCall.execute()).thenThrow(expected);
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

    // Act
    try {
      new HttpClient(mockHttpClient).partialGet(url, ranges, Collections.<String, Credentials>emptyMap(), mockReceiver,
          listener);
    } catch (IOException exception) {

      // Assert
      assertEquals("IO", exception.getMessage());
    }
  }

  @SuppressWarnings("unchecked")
  @Test
  public void runtimeExceptionThrownForHttpResponsesOtherThan206() throws Exception {
    // Arrange
    List<Integer> responsesToTest = Lists.newArrayList(500, 413); // Add whatever other ones we want
    OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    RangeReceiver mockReceiver = mock(RangeReceiver.class);
    RangeTransferListener listener = mock(RangeTransferListener.class);
    when(listener.newTransfer(any(List.class))).thenReturn(mock(HttpTransferListener.class));
    List<ContentRange> ranges = this.createSomeRanges(1);
    URI url = new URI("http://host/someurl");

    for (Integer responseToTest : responsesToTest) {

      // Arrange some more
      Call mockCall = mock(Call.class);
      Response fakeResponse = this.fakeResponse(responseToTest);
      when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);
      when(mockCall.execute()).thenReturn(fakeResponse);

      // Act
      try {
        new HttpClient(mockHttpClient).partialGet(url, ranges, Collections.<String, Credentials>emptyMap(),
            mockReceiver, listener);
      } catch (IOException exception) {

        // Assert
        assertEquals("Http request for resource " + url + " returned unexpected http code: " + responseToTest,
            exception.getMessage());
      }
    }

  }

  private Response fakeResponse(int code) {
    Request fakeRequest = new Request.Builder().url("http://host/url").build();
    return new Response.Builder().protocol(Protocol.HTTP_2).request(fakeRequest).code(code).build();
  }

  @Test
  public void testTransferListener() throws IOException {
    final URI uri = URI.create("http://host/bla");

    final byte[] data = new byte[17];
    final ResponseBody body = mock(ResponseBody.class);
    when(body.contentLength()).thenReturn((long) data.length);
    when(body.source()).thenReturn(mock(BufferedSource.class));
    final InputStream inputStream = new ByteArrayInputStream(data);
    when(body.byteStream()).thenReturn(inputStream);

    final Request request = new Request.Builder().url(uri.toString()).build();
    final Response response = new Response.Builder().protocol(HTTP_1_1).body(body).request(request).code(200).build();

    final Call mockCall = mock(Call.class);
    when(mockCall.execute()).thenReturn(response);
    final OkHttpClient mockHttpClient = mock(OkHttpClient.class);
    when(mockHttpClient.newCall(any(Request.class))).thenReturn(mockCall);

    final EventLogHttpTransferListener listener = new EventLogHttpTransferListener();
    final InputStream in =
        new HttpClient(mockHttpClient).get(uri, Collections.<String, Credentials>emptyMap(), listener);
    final byte[] b = new byte[8];
    assertEquals(0, in.read());
    assertEquals(8, in.read(b));
    assertEquals(8, in.read(b, 0, 8));
    assertEquals(-1, in.read());
    in.close();

    final List<Event> events =
        ImmutableList.of(new Initialized(new Request.Builder().url(uri.toString()).build()), new Started(uri,
            data.length), new Transferred(1), new Transferred(8), new Transferred(8), Closed.INSTANCE);
    assertEquals(events, listener.getEventLog());
  }

  @Test
  public void testChallenges() throws IOException {
    final URI uri = URI.create("https://host/file");
    final Map<String, Credentials> credentials = ImmutableMap.of("host", new Credentials("jdoe", "secret"));
    final MockOkHttpClient okHttpClient = new MockOkHttpClient();
    final HttpClient httpClient = new HttpClient(okHttpClient);
    final HttpTransferListener listener = mock(HttpTransferListener.class);

    // expect request without authorization header at first and then with
    okHttpClient.setNewCall(challengeSequence(uri, credentials));
    httpClient.get(uri, credentials, listener);

    // subsequent https calls to same host should auth right away without challenge
    okHttpClient.setNewCall(newAuthorizedRequestCall(uri, credentials));
    httpClient.get(uri, credentials, listener);

    // subsequent http calls should go not auth right away (to give a chance to redirect to https)
    final URI httpUri = URI.create("http://host/file");
    okHttpClient.setNewCall(challengeSequence(httpUri, credentials));
    httpClient.get(httpUri, credentials, listener);
  }

  Function<Request, Call> newUnauthorizedRequestCall(final URI uri) {
    final Function<Request, Call> noAuthRequest = new Function<Request, Call>() {
      @Override
      public Call apply(Request request) {
        assertEquals(uri.toString(), request.urlString());
        assertEquals("GET", request.method());
        assertNull(request.header("Authorization"));

        final Response response =
            new Response.Builder().header("WWW-Authenticate", "BASIC realm=\"global\"").code(HTTP_UNAUTHORIZED)
                .request(request).protocol(HTTP_1_1).build();
        Call call = mock(Call.class);
        try {
          when(call.execute()).thenReturn(response);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
        return call;
      }
    };
    return noAuthRequest;
  }

  Function<Request, Call> newAuthorizedRequestCall(final URI uri, final Map<String, Credentials> credentials) {
    return new Function<Request, Call>() {
      @Override
      public Call apply(Request request) {
        assertEquals(uri.toString(), request.urlString());
        assertEquals("GET", request.method());
        assertEquals(credentials.get(uri.getHost()).basic(), request.header("Authorization"));

        try {
          ResponseBody body = mock(ResponseBody.class);
          when(body.source()).thenReturn(mock(BufferedSource.class));
          when(body.byteStream()).thenReturn(mock(InputStream.class));
          final Response response =
              new Response.Builder().code(HTTP_OK).body(body).request(request).protocol(HTTP_1_1).build();
          final Call call = mock(Call.class);
          when(call.execute()).thenReturn(response);
          return call;
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    };
  }

  Function<Request, Call> challengeSequence(final URI uri, final Map<String, Credentials> credentials) {
    return sequence(newUnauthorizedRequestCall(uri), newAuthorizedRequestCall(uri, credentials));
  }

  @SafeVarargs
  final Function<Request, Call> sequence(final Function<Request, Call>... calls) {
    return new Function<Request, Call>() {
      int i = 0;

      @Override
      public Call apply(Request input) {
        return calls[this.i++].apply(input);
      }
    };
  }

  private static class MockOkHttpClient extends OkHttpClient {
    private Function<? super Request, ? extends Call> newCall;

    public void setNewCall(Function<? super Request, ? extends Call> newCall) {
      this.newCall = newCall;
    }

    @Override
    public Call newCall(Request request) {
      return this.newCall.apply(request);
    }
  }

  private List<ContentRange> createSomeRanges(int numberOfRangesToCreate) {
    List<ContentRange> ranges = new ArrayList<>(numberOfRangesToCreate);
    int rangeStart = 0;
    int rangeSize = 10;
    for (int i = 0; i < numberOfRangesToCreate; i++) {
      rangeStart = i * rangeSize;
      int rangeEnd = rangeStart + rangeSize - 1;
      ranges.add(new ContentRange(rangeStart, rangeEnd));
    }
    return ranges;
  }

}
