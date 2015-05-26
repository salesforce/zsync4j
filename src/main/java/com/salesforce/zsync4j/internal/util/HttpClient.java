package com.salesforce.zsync4j.internal.util;

import static com.google.common.base.Joiner.on;
import static com.google.common.base.Preconditions.checkArgument;
import static com.salesforce.zsync4j.internal.util.ZsyncUtil.mkdirs;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.collect.Iterables;
import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import com.salesforce.zsync4j.internal.Range;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;
import com.squareup.okhttp.ResponseBody;

public class HttpClient {

  public static interface TransferListener {

    void beginTransfer(String uri, long totalBytes);

    void transferred(int bytes);

    void done();

  }

  public static interface RangeReceiver {
    void receive(Range range, InputStream in) throws IOException;
  }

  static class ListeningInputStream extends FilterInputStream {

    private final TransferListener listener;

    public ListeningInputStream(InputStream in, TransferListener listener, Response response) {
      super(in);
      this.listener = listener;
      this.listener.beginTransfer(response.request().urlString(), response.body().contentLength());
    }

    @Override
    public int read() throws IOException {
      final int i = super.read();
      if (i >= 0) {
        this.listener.transferred(1);
      }
      return i;
    }

    @Override
    public int read(byte[] b) throws IOException {
      final int i = super.read(b);
      if (i >= 0) {
        this.listener.transferred(i);
      }
      return i;
    }

    @Override
    public int read(byte[] b, int off, int len) throws IOException {
      final int i = super.read(b, off, len);
      if (i >= 0) {
        this.listener.transferred(i);
      }
      return i;
    }

    @Override
    public void close() throws IOException {
      super.close();
      this.listener.done();
    }
  }

  private static final int MAXIMUM_RANGE_REQUESTS_PER_HTTP_REQUEST = 50;

  private final OkHttpClient okHttpClient;

  public HttpClient(OkHttpClient okHttpClient) {
    checkArgument(okHttpClient != null, "httpClient cannot be null");
    this.okHttpClient = okHttpClient;
  }

  // TODO conditional request and resume
  public InputStream get(URI uri, Path output, TransferListener listener) throws IOException {
    final Path parent = output.getParent();
    final Path tmp = parent.resolve(output.getFileName() + ".part");
    mkdirs(parent);
    try (InputStream in = get(uri, listener)) {
      Files.copy(in, tmp, REPLACE_EXISTING);
    }
    Files.move(tmp, output, REPLACE_EXISTING, ATOMIC_MOVE);
    return Files.newInputStream(output);
  }

  public InputStream get(URI uri, TransferListener listener) throws IOException {
    final Request request = new Request.Builder().url(uri.toString()).build();
    final Response response = this.okHttpClient.newCall(request).execute();

    switch (response.code()) {
      case 200:
        break;
      case 404:
        throw new FileNotFoundException(uri.toString());
      default:
        throw new IOException("Http request for resource " + uri + " returned unexpected http code: " + response.code());
    }

    return inputStream(response, listener);
  }

  public void partialGet(URI uri, List<Range> ranges, RangeReceiver receiver, TransferListener listener)
      throws IOException {
    final Set<Range> remaining = new LinkedHashSet<>(ranges);
    while (!remaining.isEmpty()) {
      final int limit = Math.min(remaining.size(), MAXIMUM_RANGE_REQUESTS_PER_HTTP_REQUEST);
      final String rangeHeader = "bytes=" + on(',').join(Iterables.limit(remaining, limit));

      final Request request = new Request.Builder().addHeader("Range", rangeHeader).url(uri.toString()).build();
      final Response response = this.okHttpClient.newCall(request).execute();

      switch (response.code()) {
        case 206:
          break;
        case 200:
          receiver.receive(new Range(0, response.body().contentLength()), inputStream(response, listener));
          return;
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
        this.handleMultiPartBody(response, receiver, remaining, listener, boundary);
      } else {
        this.handleSinglePartBody(response, receiver, remaining, listener);
      }
    }
  }

  void handleSinglePartBody(Response response, RangeReceiver receiver, final Set<Range> remaining,
      TransferListener listener) throws IOException {
    final String contentRange = response.header("Content-Range");
    if (contentRange == null) {
      throw new IOException("Content-Range header missing");
    }

    final Range range = parseContentRange(contentRange);
    if (!remaining.remove(range)) {
      throw new IOException("Received range " + range + " not one of requested " + remaining);
    }

    InputStream in = inputStream(response, listener);
    receiver.receive(range, in);
  }

  void handleMultiPartBody(Response response, RangeReceiver receiver, final Set<Range> remaining,
      TransferListener listener, byte[] boundary) {
    try (InputStream in = inputStream(response, listener); InputStream buffered = new BufferedInputStream(in)) {
      Range range;
      while ((range = this.nextPart(buffered, boundary)) != null) {
        // technically it's OK for server to combine or re-order ranges. However, since we
        // already combine and sort ranges, this should not happen
        if (!remaining.remove(range)) {
          throw new RuntimeException("Received range " + range + " not one of requested " + remaining);
        }
        final InputStream part = ByteStreams.limit(buffered, range.size());
        receiver.receive(range, part);
      }
    } catch (IOException e) {
      throw new RuntimeException("Failed to read response", e);
    }
  }

  private static InputStream inputStream(Response response, TransferListener listener) {
    final ResponseBody body = response.body();
    final InputStream in = body.byteStream();
    return new ListeningInputStream(in, listener, response);
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
