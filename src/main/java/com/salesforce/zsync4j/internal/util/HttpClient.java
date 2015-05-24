package com.salesforce.zsync4j.internal.util;

import static com.google.common.base.Preconditions.checkArgument;
import static com.salesforce.zsync4j.internal.util.ZsyncUtil.mkdirs;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import com.salesforce.zsync4j.internal.EventManager;
import com.salesforce.zsync4j.internal.Range;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public class HttpClient {

  public static interface RangeReceiver {
    void receive(Range range, InputStream in) throws IOException;
  }

  private static final int MAXIMUM_RANGE_REQUESTS_PER_HTTP_REQUEST = 50;

  private final OkHttpClient okHttpClient;

  public HttpClient(OkHttpClient okHttpClient) {
    checkArgument(okHttpClient != null, "httpClient cannot be null");
    this.okHttpClient = okHttpClient;
  }

  // TODO conditional request and resume
  public InputStream get(URI uri, ProgressMonitor monitor, Path output) throws IOException {
    final Path parent = output.getParent();
    final Path tmp = parent.resolve(output.getFileName() + ".part");
    mkdirs(parent);
    try (InputStream in = get(uri, monitor)) {
      Files.copy(in, tmp, REPLACE_EXISTING);
    }
    Files.move(tmp, output, REPLACE_EXISTING, ATOMIC_MOVE);
    return Files.newInputStream(output);
  }

  public InputStream get(URI uri, ProgressMonitor monitor) throws IOException {
    final Request request = new Request.Builder().url(uri.toString()).build();
    final Response response = okHttpClient.newCall(request).execute();

    switch (response.code()) {
      case 200:
        break;
      case 404:
        throw new FileNotFoundException(uri.toString());
      default:
        throw new IOException("Http request for resource " + uri + " returned unexpected http code: " + response.code());
    }

    return inputStream(response, monitor);
  }


  public void partialGet(URI uri, List<Range> allRanges, RangeReceiver receiver, ProgressMonitor monitor)
      throws IOException {
    List<List<Range>> chunkedRanges = Lists.partition(allRanges, MAXIMUM_RANGE_REQUESTS_PER_HTTP_REQUEST);
    long expectedBytes = 0;
    for (Range range : allRanges) {
      expectedBytes += range.size();
    }
    this.events.remoteFileProcessingStarted(url, expectedBytes, allRanges.size(), chunkedRanges.size());
    for (List<Range> rangeChunk : chunkedRanges) {
      partialGetInternal(uri, rangeChunk, receiver, monitor);
    }
    this.events.remoteFileProcessingComplete();
  }

  private void partialGetInternal(URI uri, List<Range> ranges, RangeReceiver receiver, ProgressMonitor monitor)
      throws IOException {
    final Set<Range> remaining = new LinkedHashSet<>(ranges);

    while (!remaining.isEmpty()) {
      final Request request =
          new Request.Builder().addHeader("Range", "bytes=" + toString(remaining)).url(uri.toString()).build();
      final Response response = okHttpClient.newCall(request).execute();

      // TODO if the server returns 200, we may want to overwrite the whole file locally
      switch (response.code()) {
        case 206:
          break;
        case 404:
          throw new FileNotFoundException(uri.toString());
        default:
          throw new IOException("Http request for resource " + uri + " returned unexpected http code: "
              + response.code());
      }

      final String contentType = response.header("Content-Type");
      if (contentType == null) {
        throw new RuntimeException("Missing Content-Type header");
      }
      final MediaType mediaType = MediaType.parse(contentType);
      if ("multipart".equals(mediaType.type())) {
        final byte[] boundary = getBoundary(mediaType);
        handleMultiPartBody(response, receiver, remaining, monitor, boundary);
      } else {
        handleSinglePartBody(response, receiver, remaining, monitor);
      }
    }
  }

  void handleSinglePartBody(Response response, RangeReceiver receiver, final Set<Range> remaining,
      ProgressMonitor monitor) throws IOException {
    final String contentRange = response.header("Content-Range");
    if (contentRange == null)
      throw new IOException("Content-Range header missing");

    final Range range = parseContentRange(contentRange);
    if (!remaining.remove(range))
      throw new IOException("Received range " + range + " not one of requested " + remaining);

    InputStream in = response.body().byteStream();
    if (monitor != null)
      in = new ProgressMonitorInputStream(in, range.size(), monitor);
    receiver.receive(range, in);
  }

  void handleMultiPartBody(Response response, RangeReceiver receiver, final Set<Range> remaining,
      ProgressMonitor monitor, byte[] boundary) {
    try (InputStream in = inputStream(response, monitor); InputStream buffered = new BufferedInputStream(in)) {
      Range range;
      while ((range = nextPart(buffered, boundary)) != null) {
        // technically it's OK for server to combine or re-order ranges. However, since we
        // already combine and sort ranges, this should not happen
        if (!remaining.remove(range))
          throw new RuntimeException("Received range " + range + " not one of requested " + remaining);

        final InputStream part = ByteStreams.limit(buffered, range.size());
        receiver.receive(range, part);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read response", e);
    }
    this.events.blocksRequestComplete(ranges);
  }

  private InputStream inputStream(Response response, ProgressMonitor monitor) {
    final ResponseBody body = response.body();
    final InputStream in = body.byteStream();
    return monitor == null ? in : new ProgressMonitorInputStream(in, body.contentLength(), monitor);
  }

  private Range nextPart(InputStream in, byte[] boundary) throws IOException {
    int c = in.read();
    if (c == '\r') {
      if (!(in.read() == '\n' && in.read() == '-' && in.read() == '-')) {
        throw new RuntimeException("Expected part being not matched");
      }
    } else if (c == '-') {
      if (!(in.read() == '-')) {
        throw new RuntimeException("Expected part being not matched");
      }
    }
    final byte[] b = new byte[boundary.length];
    int read = 0, r;
    while (read < b.length && (r = in.read(b, read, b.length - read)) != -1) {
      read += r;
    }
    if (read != b.length || !Arrays.equals(boundary, b)) {
      throw new RuntimeException("Invalid multipart boundary");
    }
    final int r1 = in.read();
    final int r2 = in.read();
    if (r1 == '-' && r2 == '-') {
      if (!(in.read() == '\r' && in.read() == '\n' && in.read() == -1)) {
        throw new RuntimeException("unexpected end of body");
      }
      return null;
    } else if (!(r1 == '\r' && r2 == '\n')) {
      throw new RuntimeException("Missing control line feed");
    }

    Range range = null;
    String header;
    while ((header = readHeader(in)) != null) {
      if (header.startsWith("Content-Range")) {
        if (range != null) {
          throw new RuntimeException("Multiple content range headers in multipart");
        }
        int idx = header.indexOf(':');
        if (idx == -1) {
          throw new RuntimeException("Invalid Content-Range header " + header + " in multipart");
        }
        range = parseContentRange(header.substring(idx + 2));
      }
    }
    return range;
  }

  private static String readHeader(InputStream in) throws IOException {
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
    throw new RuntimeException("Body ended before finding multipart delimiter");
  }

  static byte[] getBoundary(final MediaType mediaType) {
    if (!"byteranges".equals(mediaType.subtype())) {
      throw new RuntimeException("Invalid multipart subtype " + mediaType.subtype());
    }
    final List<String> value = mediaType.parameters().get("boundary");
    if (value == null || value.isEmpty()) {
      throw new RuntimeException("Missing multipart boundary parameter");
    }
    final byte[] boundary = value.get(0).getBytes(ISO_8859_1);
    return boundary;
  }

  static String toString(final Iterable<? extends Range> ranges) {
    final Iterator<? extends Range> it = ranges.iterator();
    if (!it.hasNext()) {
      throw new RuntimeException("no ranges");
    }
    final StringBuilder b = new StringBuilder(it.next().toString());
    while (it.hasNext()) {
      b.append(",").append(it.next().toString());
    }
    return b.toString();
  }

  static Range parseContentRange(String value) {
    final String prefix = "bytes ";
    if (!value.startsWith(prefix)) {
      throw new IllegalArgumentException("Invalid Content-Range value " + value);
    }
    final int idx = value.indexOf('-', prefix.length());
    if (idx <= 0) {
      throw new IllegalArgumentException("Invalid Content-Range value " + value);
    }
    final long first = Long.parseLong(value.substring(prefix.length(), idx));
    final int dash = value.indexOf('/', idx);
    if (idx <= 0) {
      throw new IllegalArgumentException("Invalid Content-Range value " + value);
    }
    final long last = Long.parseLong(value.substring(idx + 1, dash));
    final Range range = new Range(first, last);
    final long size = Long.parseLong(value.substring(dash + 1));
    if (size != range.size()) {
      // TODO - Need to review this next line
      // throw new IllegalArgumentException("Invalid Content-Range size " + value);
    }
    return range;
  }
}
