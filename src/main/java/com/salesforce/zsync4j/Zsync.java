package com.salesforce.zsync4j;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.List;

import com.google.common.base.Stopwatch;
import com.salesforce.zsync4j.internal.BlockMatcher;
import com.salesforce.zsync4j.internal.ControlFile;
import com.salesforce.zsync4j.internal.Range;
import com.salesforce.zsync4j.internal.TargetFile;
import com.salesforce.zsync4j.internal.util.RangeFetcher;
import com.salesforce.zsync4j.internal.util.RollingBuffer;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

public class Zsync {

  public static final String VERSION = "0.6.2";

  private final OkUrlFactory httpUrlFactory;

  public Zsync(OkHttpClient httpClient) {
    this.httpUrlFactory = new OkUrlFactory(httpClient);
  }

  public void zsync(URI zsyncFile, Path inputFile) {
    final Stopwatch s = Stopwatch.createStarted();

    final ControlFile controlFile;
    try (InputStream in = toURL(zsyncFile).openStream()) {
      controlFile = ControlFile.read(in);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read zsync control file", e);
    }

    final BlockMatcher matcher = BlockMatcher.create(controlFile);
    final Path outputFile = inputFile.getParent().resolve(controlFile.getHeader().getFilename());
    try (final TargetFile targetFile = new TargetFile(outputFile, controlFile)) {
      // first fill target file with blocks from local file
      writeLocal(targetFile, matcher, inputFile);
      // next fetch remaining blocks from remote
      writeRemote(targetFile, zsyncFile.resolve(controlFile.getHeader().getUrl()));
    } catch (IOException e) {
      throw new RuntimeException("Failed to write target file file", e);
    }

    System.out.println(s.stop());
  }

  static void writeLocal(TargetFile targetFile, BlockMatcher matcher, Path seed) throws IOException {
    // TODO pad end of input file?
    try (final FileChannel channel = FileChannel.open(seed)) {
      final RollingBuffer buffer = new RollingBuffer(channel, matcher.getMatchBytes(), 16 * matcher.getMatchBytes());
      int bytes;
      do {
        bytes = matcher.match(targetFile, buffer);
      } while (buffer.advance(bytes));
    }
  }

  void writeRemote(TargetFile targetFile, URI url) {
    final List<Range> missingRanges = targetFile.getMissingRanges();
    System.out.println("Missing ranges: " + missingRanges);
    final RangeFetcher fetcher = new RangeFetcher(httpUrlFactory.client());
    fetcher.fetch(url, missingRanges, targetFile);
  }

  private URL toURL(URI uri) throws MalformedURLException {
    final URLStreamHandler handler = httpUrlFactory.createURLStreamHandler(uri.getScheme());
    return handler == null ? uri.toURL() : new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), handler);
  }

  public static void main(String[] args) {
    if (args.length != 2)
      throw new IllegalArgumentException("wrong number of args");
    final URI uri = URI.create(args[0]);
    final Path inputFile = FileSystems.getDefault().getPath(args[1]);
    final Zsync zsync = new Zsync(new OkHttpClient());
    zsync.zsync(uri, inputFile);
  }
}
