package com.salesforce.zsync4j.internal.util;

import static java.nio.charset.StandardCharsets.ISO_8859_1;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import com.google.common.io.ByteStreams;
import com.google.common.net.MediaType;
import com.salesforce.zsync4j.internal.Range;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

public class RangeFetcher {

  public static interface RangeReceiver {
    void receive(Range range, InputStream in) throws IOException;
  }

  private final OkHttpClient httpClient;

  public RangeFetcher(OkHttpClient httpClient) {
    this.httpClient = httpClient;
  }

  public void fetch(URI url, List<Range> ranges, RangeReceiver receiver) {
    final Set<Range> remaining = new LinkedHashSet<>(ranges);

    while (!remaining.isEmpty()) {
      // TODO limit ranges sent at once
      final Request request = new Request.Builder().addHeader("Range", "bytes=" + toString(remaining)).url(url.toString()).build();
      final Response response;
      try {
        response = httpClient.newCall(request).execute();
      } catch (IOException e) {
        throw new RuntimeException(e);
      }

      switch (response.code()) {
        case 206:
          break;
        default:
          throw new RuntimeException("Http request failed with error code " + response.code());
      }

      final String contentType = response.header("Content-Type");
      if (contentType == null)
        throw new RuntimeException("Missing Content-Type header");
      final MediaType mediaType = MediaType.parse(contentType);

      if ("multipart".equals(mediaType.type())) {
        final byte[] boundary = getBoundary(mediaType);

        try (InputStream in = new BufferedInputStream(response.body().byteStream())) {
          Range range;
          while ((range = nextPart(in, boundary)) != null) {
            // technically it's OK for server to combine or re-order ranges. However, since we
            // already combine and sort ranges, this should not happen
            if (!remaining.remove(range))
              throw new RuntimeException("Received range " + range + " not one of requested " + remaining);
            final InputStream part = ByteStreams.limit(in, range.size());
            receiver.receive(range, part);
          }
        } catch (IOException e) {
          throw new RuntimeException("Failed to read response", e);
        }
      } else {
        final String contentRange = response.header("Content-Range");
        if (contentRange == null)
          throw new RuntimeException("Content-Range header missing");
        final Range range = parseContentRange(contentRange);
        if (!remaining.remove(range))
          throw new RuntimeException("Received range " + range + " not one of requested " + remaining);
        try {
          receiver.receive(range, response.body().byteStream());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }
    }
  }

  private Range nextPart(InputStream in, byte[] boundary) throws IOException {
    if (!(in.read() == '\r' && in.read() == '\n' && in.read() == '-' && in.read() == '-'))
      throw new RuntimeException("Expected part being not matched");
    final byte[] b = new byte[boundary.length];
    if (in.read(b) == -1)
      throw new RuntimeException("Premature end of body");
    if (!Arrays.equals(boundary, b))
      throw new RuntimeException("Invalid multipart boundary");
    final int r1 = in.read();
    final int r2 = in.read();
    if (r1 == '-' && r2 == '-') {
      if (!(in.read() == '\r' && in.read() == '\n' && in.read() == -1))
        throw new RuntimeException("unexpected end of body");
      return null;
    } else if (!(r1 == '\r' && r2 == '\n'))
      throw new RuntimeException("Missing control line feed");

    Range range = null;
    String header;
    while ((header = readHeader(in)) != null) {
      if (header.startsWith("Content-Range")) {
        if (range != null)
          throw new RuntimeException("Multiple content range headers in multipart");
        int idx = header.indexOf(':');
        if (idx == -1)
          throw new RuntimeException("Invalid Content-Range header " + header + " in multipart");
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
      if (prev == '\r' && read == '\n')
        return pos == 1 ? null : new String(buf, 0, pos - 1, ISO_8859_1);
      if (pos == buf.length)
        buf = Arrays.copyOf(buf, buf.length * 2);
      prev = (byte) read;
      buf[pos++] = prev;
    }
    throw new RuntimeException("Body ended before finding multipart delimiter");
  }

  static byte[] getBoundary(final MediaType mediaType) {
    if (!"byteranges".equals(mediaType.subtype()))
      throw new RuntimeException("Invalid multipart subtype " + mediaType.subtype());
    final List<String> value = mediaType.parameters().get("boundary");
    if (value == null || value.isEmpty())
      throw new RuntimeException("Missing multipart boundary parameter");
    final byte[] boundary = value.get(0).getBytes(ISO_8859_1);
    return boundary;
  }

  static String toString(final Iterable<? extends Range> ranges) {
    final Iterator<? extends Range> it = ranges.iterator();
    if (!it.hasNext())
      throw new RuntimeException("no ranges");
    final StringBuilder b = new StringBuilder(it.next().toString());
    while (it.hasNext())
      b.append(",").append(it.next().toString());
    return b.toString();
  }

  static Range parseContentRange(String value) {
    final String prefix = "bytes ";
    if (!value.startsWith(prefix))
      throw new IllegalArgumentException("Invalid Content-Range value " + value);
    final int idx = value.indexOf('-', prefix.length());
    if (idx == 0)
      throw new IllegalArgumentException("Invalid Content-Range value " + value);
    final long first = Long.parseLong(value.substring(prefix.length(), idx));
    final int dash = value.indexOf('/', idx);
    if (idx == 0)
      throw new IllegalArgumentException("Invalid Content-Range value " + value);
    final long last = Long.parseLong(value.substring(idx + 1, dash));
    final Range range = new Range(first, last);
    final long size = Long.parseLong(value.substring(dash + 1));
    if (size != range.size())
      throw new IllegalArgumentException("Invalid Content-Range size " + value);
    return range;
  }
}
