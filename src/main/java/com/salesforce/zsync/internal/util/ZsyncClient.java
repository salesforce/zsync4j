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

import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import com.salesforce.zsync.http.ContentRange;
import com.salesforce.zsync.http.Credentials;
import com.salesforce.zsync.internal.util.ObservableInputStream.ObservableResourceInputStream;
import com.salesforce.zsync.internal.util.TransferListener.ResourceTransferListener;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.collect.ImmutableList.copyOf;
import static com.google.common.collect.Iterables.limit;
import static java.lang.Math.min;
import static java.net.HttpURLConnection.*;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.util.Collections.newSetFromMap;


/**
 * A thin wrapper around {@link HttpClient} to facilitate full and partial download of resources
 * with
 *
 * @author bbusjaeger
 * @author Vladimir Djurovic
 */
public class ZsyncClient {

  public static ZsyncClient newZsyncClient() {
    return new ZsyncClient(HttpClient.newHttpClient());
  }


  /**
   * Indicates that an unexpected response code has been received
   *
   * @author bbusjaeger
   */
  public static class HttpError extends Exception {
    private static final long serialVersionUID = 8433444973591504743L;

    private final int code;

    public HttpError(String message, int code) {
      super(message);
      this.code = code;
    }

    public int getCode() {
      return this.code;
    }
  }

  /**
   * Emits an <code>initiating</code> event prior to sending the Http request to the sever. Once the
   * response has been received and the header parsed, emits a <code>started</code> event prior to
   * transferring the response body. The length may be -1 if the content length is unknown.
   *
   * @author bbusjaeger
   */
  public  interface HttpTransferListener extends ResourceTransferListener<HttpResponse<byte[]>> {
    void initiating(HttpRequest request);
  }

  public static interface RangeTransferListener {
    HttpTransferListener newTransfer(List<ContentRange> ranges);
  }

  public static interface RangeReceiver {
    void receive(ContentRange range, InputStream in) throws IOException;
  }

  private static final int MAXIMUM_RANGES_PER_HTTP_REQUEST = 100;

  private final HttpClient httpClient;
  private final Set<String> basicChallengeReceived;

  ZsyncClient(HttpClient httpClient) {
    checkArgument(httpClient != null, "httpClient cannot be null");
    this.httpClient = httpClient;
    this.basicChallengeReceived = newSetFromMap(new ConcurrentHashMap<String, Boolean>());
  }

  /**
   * Stores the resource referred to by the given uri at the given output location. Progress of the
   * file download can be monitored via the optional transfer listener.
   *
   * @param uri
   * @param output
   * @param credentials
   * @param listener
   * @throws IOException
   * @throws HttpError
   */
  public void get(URI uri, Path output, Map<String, ? extends Credentials> credentials, HttpTransferListener listener)
      throws IOException, HttpError, InterruptedException {
    final Path parent = output.getParent();
    final Path tmp = parent.resolve(output.getFileName() + ".part");
    Files.createDirectories(parent);
    try (InputStream in = this.get(uri, credentials, listener)) {
      Files.copy(in, tmp, REPLACE_EXISTING);
    }
    Files.move(tmp, output, REPLACE_EXISTING, ATOMIC_MOVE);
  }

  /**
   * Opens a connection to the remote resource referred to by the given uri. The returned stream is
   * decorated with to report download progress to the given listener.
   *
   * @param uri The URI of the resource to retrieve
   * @param credentials The credentials for authenticating with remote hosts
   * @param listener Listener to monitor long running transfers
   * @return
   * @throws IOException
   * @throws HttpError
   */
  public InputStream get(URI uri, Map<String, ? extends Credentials> credentials, HttpTransferListener listener)
      throws IOException, HttpError, InterruptedException {
    final HttpResponse<byte[]> response = executeWithAuthRetry(uri, credentials, listener, Collections.<ContentRange>emptyList());
    final int code = response.statusCode();
    if (code != HTTP_OK) {
      throw new HttpError("Request failed", code);
    }
    return inputStream(response, listener);
  }

  /**
   * Retrieves the requested ranges for the resource referred to by the given uri.
   *
   * @param uri
   * @param ranges
   * @param receiver
   * @param listener
   * @throws IOException
   * @throws HttpError
   */
  public void partialGet(URI uri, List<ContentRange> ranges, Map<String, ? extends Credentials> credentials,
      RangeReceiver receiver, RangeTransferListener listener) throws IOException, HttpError, InterruptedException {
    final Set<ContentRange> remaining = new LinkedHashSet<>(ranges);
    while (!remaining.isEmpty()) {
      final List<ContentRange> next = copyOf(limit(remaining, min(remaining.size(), MAXIMUM_RANGES_PER_HTTP_REQUEST)));
      final HttpTransferListener requestListener = listener.newTransfer(next);
      final HttpResponse<byte[]> response = executeWithAuthRetry(uri, credentials, requestListener, next);
      final int code = response.statusCode();
      // tolerate case that server does not support range requests
      if (code == HTTP_OK) {
        receiver.receive(new ContentRange(0, response.body().length), inputStream(response, requestListener));
        return;
      }
      // otherwise only accept partial content response
      if (code != HTTP_PARTIAL) {
        throw new HttpError("Not partial HTTP code", code);
      }
      // check if we're dealing with multipart (multiple ranges) or simple (single range) response
      final MediaType mediaType = parseContentType(response);
      if (mediaType != null && "multipart".equals(mediaType.type())) {
        final byte[] boundary = getBoundary(mediaType);
        handleMultiPartBody(response, receiver, remaining, requestListener, boundary);
      } else {
        handleSinglePartBody(response, receiver, remaining, requestListener);
      }
    }
  }

  HttpResponse<byte[]> executeWithAuthRetry(URI uri, Map<String, ? extends Credentials> credentials, HttpTransferListener listener,
                                                 List<ContentRange> ranges) throws IOException, InterruptedException {
    HttpRequest request = buildRequest(uri, credentials, ranges);
    listener.initiating(request);
    HttpResponse<byte[]> response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    for (int i = 0; i < 10; i++) {
      final int code = response.statusCode();
      if (!((code == HTTP_UNAUTHORIZED || code == HTTP_PROXY_AUTH) && containsBasic(response.headers().firstValue("WWW-Authenticate").orElse("")))) {
        break;
      }
      // if we are receiving a basic authorization challenges, set header and retry
      final String host = response.request().uri().getHost();
      this.basicChallengeReceived.add(host);
      final Credentials creds = credentials.get(host);
      if (creds == null) {
        break;
      }
      final String name = code == HTTP_UNAUTHORIZED ? "Authorization" : "Proxy-Authorization";
      request = response.request().newBuilder().header(name, creds.basic()).build();
      response = this.httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
    }
    return response;
  }

  HttpRequest buildRequest(URI uri, Map<String, ? extends Credentials> credentials, List<ContentRange> ranges) {
    final HttpRequest.Builder builder = HttpRequest.newBuilder();
    builder.uri(uri);
    if (this.basicChallengeReceived.contains(uri.getHost()) && "https".equals(uri.getScheme())) {
      final Credentials creds = credentials.get(uri.getHost());
      if (creds != null) {
        builder.header("Authorization", creds.basic());
      }
    }
    if (!ranges.isEmpty()) {
      builder.header("Range", "bytes=" + on(',').join(ranges));
    }
    return builder.build();
  }

  static boolean containsBasic(String challenge) {
    return challenge.trim().startsWith("Basic");
  }

  static void handleSinglePartBody(HttpResponse<byte[]> response, RangeReceiver receiver, final Set<ContentRange> remaining,
      HttpTransferListener listener) throws IOException {
    final String contentRange = response.headers().firstValue("Content-Range").orElse(null);
    if (contentRange == null) {
      throw new IOException("Content-Range header missing");
    }

    ContentRange range;
    try {
      range = parseContentRange(contentRange);
    } catch (ParseException e) {
      throw new IOException("Failed to parse Content-Range header " + contentRange, e);
    }
    if (!remaining.remove(range)) {
      throw new IOException("Received range " + range + " not one of requested " + remaining);
    }

    InputStream in = inputStream(response, listener);
    receiver.receive(range, in);
  }

  static void handleMultiPartBody(HttpResponse<byte[]> response, RangeReceiver receiver, final Set<ContentRange> remaining,
      HttpTransferListener listener, byte[] boundary) throws IOException {
    try (InputStream in = inputStream(response, listener)) {
      ContentRange range;
      while ((range = nextPart(in, boundary)) != null) {
        // technically it's OK for server to combine or re-order ranges. However, since we
        // already combine and sort ranges, this should not happen
        if (!remaining.remove(range)) {
          throw new IOException("Received range " + range + " not one of requested " + remaining);
        }
        final InputStream part = ByteStreams.limit(in, range.length());
        receiver.receive(range, part);
      }
    }
  }

  static InputStream inputStream(HttpResponse<byte[]> response, ResourceTransferListener<HttpResponse<byte[]>> listener) throws IOException {

    final InputStream in = new ByteArrayInputStream(response.body());
    return new ObservableResourceInputStream<>(in, listener, response, response.body().length);
  }

  static ContentRange nextPart(InputStream in, byte[] boundary) throws IOException {
    int c = in.read();
    if (c == '\r') {
      if (!(in.read() == '\n' && in.read() == '-' && in.read() == '-')) {
        throw new IOException("Expected part being not matched");
      }
    } else if (c == '-') {
      if (!(in.read() == '-')) {
        throw new IOException("Expected part being not matched");
      }
    }
    final byte[] b = new byte[boundary.length];
    int read = 0, r;
    while (read < b.length && (r = in.read(b, read, b.length - read)) != -1) {
      read += r;
    }
    if (read != b.length || !Arrays.equals(boundary, b)) {
      throw new IOException("Invalid multipart boundary");
    }
    final int r1 = in.read();
    final int r2 = in.read();
    if (r1 == '-' && r2 == '-') {
      if (!(in.read() == '\r' && in.read() == '\n' && in.read() == -1)) {
        throw new IOException("unexpected end of body");
      }
      return null;
    } else if (!(r1 == '\r' && r2 == '\n')) {
      throw new IOException("Missing control line feed");
    }

    ContentRange range = null;
    String header;
    while ((header = readHeader(in)) != null) {
      if (header.startsWith("Content-Range") || header.startsWith("Content-range")) {
        if (range != null) {
          throw new IOException("Multiple content range headers in multipart");
        }
        int idx = header.indexOf(':');
        if (idx == -1) {
          throw new IOException("Invalid Content-Range header " + header + " in multipart");
        }
        final String value = header.substring(idx + 2);
        try {
          range = parseContentRange(value);
        } catch (ParseException e) {
          throw new IOException("Failed to parse Content-Range header " + value, e);
        }
      }
    }
    return range;
  }

  static String readHeader(InputStream in) throws IOException {
    byte[] buf = new byte[256];
    int pos = 0;
    byte prev = -1;
    int read;
    while ((read = in.read()) != -1) {
      if (prev == '\r' && read == '\n') {
        return pos == 1 ? null : new String(buf, 0, pos - 1, ISO_8859_1);
      }
      if (pos == buf.length) {
        buf = Arrays.copyOf(buf, buf.length * 2);
      }
      prev = (byte) read;
      buf[pos++] = prev;
    }
    throw new IOException("Body ended before finding multipart delimiter");
  }

  /**
   * Returns the boundary attribtue of the given multipart/byteranges media type. If the subtype is
   * not byteranges or no boundary attribute value is set, an IOException is thrown.
   *
   * @param mediaType
   * @return
   * @throws IOException
   * @throws ParseException
   */
  static byte[] getBoundary(final MediaType mediaType) throws IOException {
    if (!"byteranges".equals(mediaType.subtype())) {
      throw new IOException("Invalid multipart subtype " + mediaType.subtype() + ", expected 'byteranges'");
    }
    final List<String> value = mediaType.parameters().get("boundary");
    if (value.isEmpty()) {
      throw new IOException("Missing multipart boundary parameter");
    }
    return value.get(0).getBytes(ISO_8859_1);
  }

  /**
   * Parses the Content-Type of the given response. If the response does not have a Content-Type
   * set, null is returned. If the value cannot be parsed, an IOException is thrown.
   *
   * @param response Response for which to parse the Content-Type
   * @return MediaType parsed from Content-Type value
   * @throws IOException If no Content-Type value cannot be parsed
   */
  static MediaType parseContentType(final HttpResponse response) throws IOException {
    final Optional<String> contentType = response.headers().firstValue("Content-Type");
    if(contentType.isPresent()) {
      try {
        return MediaType.parse(contentType.get());
      } catch (IllegalArgumentException e) {
        throw new IOException("Failed to parse Content-Type header " + contentType, e);
      }
    }
    return null;

  }

  /**
   * Parses a ContentRange from the given Content-Range header value
   *
   * @param value
   * @return
   * @throws ParseException If the ContentRange
   */
  static ContentRange parseContentRange(String value) throws ParseException {
    final String prefix = "bytes ";
    if (!value.startsWith(prefix)) {
      throw new ParseException("Unrecognized bytes-unit (only \"bytes\" supported)", 0);
    }
    final int idx = value.indexOf('-', prefix.length());
    if (idx <= 0) {
      throw new ParseException("Missing separator '-'.", prefix.length());
    }
    // parse first-byte-pos
    final String firstString = value.substring(prefix.length(), idx);
    final long first;
    try {
      first = Long.parseLong(firstString);
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid first-byte-pos " + firstString, prefix.length());
    }
    final int dash = value.indexOf('/', idx);
    if (dash <= 0) {
      throw new ParseException("Missing separator '/'", idx);
    }
    // parse last-byte-pos
    final String lastString = value.substring(idx + 1, dash);
    final long last;
    try {
      last = Long.parseLong(lastString);
    } catch (NumberFormatException e) {
      throw new ParseException("Invalid last-byte-pos " + lastString, idx + 1);
    }
    final ContentRange range;
    try {
      range = new ContentRange(first, last);
    } catch (IllegalArgumentException e) {
      throw new ParseException(e.getMessage(), 0);
    }
    return range;
  }

}
