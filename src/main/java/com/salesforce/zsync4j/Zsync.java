package com.salesforce.zsync4j;

import static com.salesforce.zsync4j.OutputFileListener.OutputFileEvent.transferEndedEvent;
import static com.salesforce.zsync4j.OutputFileListener.OutputFileEvent.transferStartedEvent;

import java.io.BufferedInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.Proxy;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

import com.google.common.base.Stopwatch;
import com.salesforce.zsync4j.Zsync.Options.Credentials;
import com.salesforce.zsync4j.internal.BlockMatcher;
import com.salesforce.zsync4j.internal.ControlFile;
import com.salesforce.zsync4j.internal.Header;
import com.salesforce.zsync4j.internal.OutputFile;
import com.salesforce.zsync4j.internal.Range;
import com.salesforce.zsync4j.internal.util.RangeFetcher;
import com.salesforce.zsync4j.internal.util.RollingBuffer;
import com.salesforce.zsync4j.internal.util.ZeroPaddedReadableByteChannel;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * Zsync download client: reduces the number of bytes retrieved from a remote server by drawing
 * unchanged parts of the file from a set of local input files.
 *
 * @see <a href="http://zsync.moria.org.uk/">http://zsync.moria.org.uk/</a>
 *
 * @author bbusjaeger
 *
 */
public class Zsync {

  /**
   * Optional arguments to the zsync client.
   *
   * @see <a href="http://linux.die.net/man/1/zsync">zsync(1) - Linux man page</a>
   *
   * @author bbusjaeger
   *
   */
  public static class Options {

    /**
     * Credentials used to authenticate with remote hosts
     *
     * @author bbusjaeger
     */
    public static class Credentials {
      private final String username;
      private final String password;

      public Credentials(String username, String password) {
        this.username = username;
        this.password = password;
      }

      /**
       * User name for remote authentication
       *
       * @return
       */
      public String getUsername() {
        return this.username;
      }

      /**
       * Password for remote authentication
       *
       * @return
       */
      public String getPassword() {
        return this.password;
      }
    }

    private List<Path> inputFiles = new ArrayList<>(2);
    private Path outputFile;
    private Path saveZsyncFile;
    private URI zsyncUri;
    private Map<String, Credentials> credentials = new HashMap<>(2);

    public Options() {
      super();
    }

    public Options(Options other) {
      if (other != null) {
        this.inputFiles.addAll(other.getInputFiles());
        this.outputFile = other.outputFile;
        this.saveZsyncFile = other.saveZsyncFile;
        this.zsyncUri = other.zsyncUri;
        this.credentials.putAll(other.credentials);
      }
    }

    /**
     * Adds an input file from which matching blocks are transferred to the output file to reduce
     * the ranges that have to be fetched from the remote source.
     *
     * @param inputFile
     * @return
     */
    public Options addInputFile(Path inputFile) {
      this.inputFiles.add(inputFile);
      return this;
    }

    /**
     * Input files to construct output file from. May be empty in which case the full content is
     * retrieved from the remote location.
     *
     * @return
     */
    public List<Path> getInputFiles() {
      return this.inputFiles;
    }

    /**
     * Corresponds to zsync -o parameter: the location at which to store the output file.
     *
     * @param outputFile
     * @return
     */
    public Options setOutputFile(Path outputFile) {
      this.outputFile = outputFile;
      return this;
    }

    /**
     * Location at which to store the output file. If not set, output will be stored in the current
     * working directory using the <code>Filename</code> header from the control file as the
     * relative path.
     *
     * @return
     */
    public Path getOutputFile() {
      return this.outputFile;
    }

    /**
     * Corresponds to the zsync -k parameter: the location at which to store the zsync control file.
     * This option only takes effect if the zsync URI passed as the first argument to
     * {@link Zsync#zsync(URI, Options)} is a remote (http) URL.
     *
     * @param saveZsyncFile
     * @return
     */
    public Options setSaveZsyncFile(Path saveZsyncFile) {
      this.saveZsyncFile = saveZsyncFile;
      return this;
    }

    /**
     * The location at which to persist the zsync file if remote, may be null
     *
     * @return
     */
    public Path getSaveZsyncFile() {
      return this.saveZsyncFile;
    }

    /**
     * Corresponds to the zsync -u parameter: the source URI from which the zsync file was
     * originally retrieved. Takes affect only if the first parameter to the
     * {@link Zsync#zsync(URI, Options)} method refers to a local file.
     *
     * @param zsyncUri
     * @return
     */
    public Options setZsyncFileSource(URI zsyncUri) {
      this.zsyncUri = zsyncUri;
      return this;
    }

    /**
     * The remote URI from which the local zsync file was originally retrieved from
     *
     * @return
     */
    public URI getZsyncFileSource() {
      return this.zsyncUri;
    }

    /**
     * Registers the given credentials for the given hostname.
     *
     * @param hostname
     * @param credentials
     * @return
     */
    public Options putCredentials(String hostname, Credentials credentials) {
      this.credentials.put(hostname, credentials);
      return this;
    }

    /**
     * Registered credentials
     *
     * @return
     */
    public Map<String, Credentials> getCredentials() {
      return this.credentials;
    }

  }

  public static final String VERSION = "0.6.2";

  private final OkUrlFactory httpUrlFactory;

  public Zsync(OkHttpClient httpClient) {
    this.httpUrlFactory = new OkUrlFactory(httpClient);
  }

  /**
   * Retrieves the remote file pointed to by the given zsync control file.
   *
   * @param zsyncFile
   * @param options
   * @param outputFileListener
   * @throws ZsyncFileNotFoundException
   * @throws OutputFileValidationException
   */
  public void zsync(URI zsyncFile, Options options) throws ZsyncFileNotFoundException, OutputFileValidationException {
    this.zsync(zsyncFile, options, OutputFileListener.NO_OP);
  }

  /**
   * Retrieves the remote file pointed to by the given zsync control file. The supplied listener is
   * called back to as data is downloaded and the output file is written.
   *
   * @param zsyncFile
   * @param options
   * @param outputFileListener
   * @throws ZsyncFileNotFoundException
   * @throws OutputFileValidationException
   */
  public void zsync(URI zsyncFile, Options options, OutputFileListener outputFileListener)
      throws ZsyncFileNotFoundException, OutputFileValidationException {
    final Stopwatch s = Stopwatch.createStarted();

    // create copy, since options mutable
    final Options o = new Options(options);

    if (outputFileListener == null) {
      outputFileListener = OutputFileListener.NO_OP;
    }

    this.setCredentials(o.getCredentials());

    final ControlFile controlFile;
    try (InputStream in = new BufferedInputStream(this.toURL(zsyncFile).openStream())) {
      controlFile = ControlFile.read(in);
    } catch (FileNotFoundException e) {
      throw new ZsyncFileNotFoundException("Zsync file " + zsyncFile + " does not exist.", e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read zsync control file", e);
    }

    final Path outputFile =
        o.getOutputFile() == null ? FileSystems.getDefault().getPath(controlFile.getHeader().getFilename()) : o
            .getOutputFile();

    URI remoteFileUri = zsyncFile.resolve(controlFile.getHeader().getUrl());

    outputFileListener.transferStarted(transferStartedEvent(outputFile, remoteFileUri, controlFile.getHeader()
        .getLength()));

    Exception exception = null;
    try (final OutputFile targetFile = new OutputFile(outputFile, controlFile, remoteFileUri, outputFileListener)) {
      if (!processInputFiles(targetFile, controlFile, o.getInputFiles())) {
        this.fetchRanges(targetFile, remoteFileUri);
      }
    } catch (OutputFileValidationException e) {
      exception = e;
      throw e;
    } catch (IOException e) {
      exception = new RuntimeException("Failed to write target file file", e);
      throw (RuntimeException) exception;
    } finally {
      outputFileListener.transferEnded(transferEndedEvent(outputFile, remoteFileUri, controlFile.getHeader()
          .getLength(), exception));
    }

    System.out.println(s.stop());
  }

  // TODO OK to set authenticator for entire client?
  void setCredentials(final Map<String, ? extends Credentials> credentials) {
    this.httpUrlFactory.client().setAuthenticator(new Authenticator() {
      @Override
      public Request authenticateProxy(Proxy proxy, Response response) throws IOException {
        return this.authenticate(proxy, response);
      }

      @Override
      public Request authenticate(Proxy proxy, Response response) throws IOException {
        final String host = response.request().uri().getHost();
        final Credentials creds = credentials.get(host);
        final Request.Builder b = response.request().newBuilder();
        if (creds != null) {
          b.header("Authorization", com.squareup.okhttp.Credentials.basic(creds.getUsername(), creds.getPassword()));
        }
        return b.build();
      }
    });
  }

  static boolean processInputFiles(OutputFile targetFile, ControlFile controlFile, Iterable<? extends Path> inputFiles)
      throws IOException {
    for (Path inputFile : inputFiles) {
      if (processInputFile(targetFile, controlFile, inputFile)) {
        return true;
      }
    }
    return false;
  }

  static boolean processInputFile(OutputFile targetFile, ControlFile controlFile, Path inputFile) throws IOException {
    try (final FileChannel channel = FileChannel.open(inputFile)) {
      final BlockMatcher matcher = BlockMatcher.create(controlFile);
      final RollingBuffer buffer =
          new RollingBuffer(zeroPad(channel, controlFile.getHeader()), matcher.getMatchBytes(),
              16 * matcher.getMatchBytes());
      int bytes;
      do {
        bytes = matcher.match(targetFile, buffer);
      } while (buffer.advance(bytes));
    }
    return targetFile.isComplete();
  }

  /**
   * Pads the given channel with zeros if the length of the input file is not evenly divisible by
   * the block size. The is necessary to match how the checksums in the zsync file are computed.
   *
   * @param channel channel for input file to pad
   * @param header header of the zsync file being processed.
   * @return
   */
  static ReadableByteChannel zeroPad(ReadableByteChannel channel, Header header) {
    final int r = (int) (header.getLength() % header.getBlocksize());
    return r == 0 ? channel : new ZeroPaddedReadableByteChannel(channel, header.getBlocksize() - r);
  }

  void fetchRanges(OutputFile targetFile, URI url) {
    final List<Range> missingRanges = targetFile.getMissingRanges();
    if (missingRanges.isEmpty()) {
      return;
    }
    final RangeFetcher fetcher = new RangeFetcher(this.httpUrlFactory.client());
    fetcher.fetch(url, missingRanges, targetFile);
  }

  private URL toURL(URI uri) throws MalformedURLException {
    final URLStreamHandler handler = this.httpUrlFactory.createURLStreamHandler(uri.getScheme());
    return handler == null ? uri.toURL() : new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(),
        handler);
  }

  // this is just a temporary hacked up CLI for testing purposes
  public static void main(String[] args) throws IOException, ZsyncFileNotFoundException {
    if (args.length == 0) {
      throw new IllegalArgumentException("Must specify at least zsync file url");
    }

    if (args.length % 2 == 0) {
      throw new IllegalArgumentException("Must specify pairs of args");
    }

    final FileSystem fs = FileSystems.getDefault();
    final Options options = new Options();
    for (int i = 0; i < args.length - 1; i++) {
      if ("-A".equals(args[i])) {
        final String auth = args[++i];
        final int eq, cl;
        if ((eq = auth.indexOf('=')) > 0 && (cl = auth.indexOf(':', eq + 1)) > 0) {
          options.putCredentials(auth.substring(0, eq),
              new Credentials(auth.substring(eq + 1, cl), auth.substring(cl + 1)));
        } else {
          throw new IllegalArgumentException("authenticator must be of form 'hostname=username:password'");
        }
      } else if ("-i".equals(args[i])) {
        options.addInputFile(fs.getPath(args[++i]));
      } else if ("-o".equals(args[i])) {
        options.setOutputFile(fs.getPath(args[++i]));
      }
    }
    final URI uri = URI.create(args[args.length - 1]);

    final Zsync zsync = new Zsync(new OkHttpClient());
    final OutputFileListener l = new OutputFileListener() {
      final AtomicLong total = new AtomicLong();
      final AtomicLong dl = new AtomicLong();

      @Override
      public void transferStarted(OutputFileEvent event) {
        this.total.set(event.getRemoteFileSizeInBytes());
      }

      @Override
      public void bytesDownloaded(OutputFileEvent event) {
        this.dl.addAndGet(event.getBytesDownloaded());
      }

      @Override
      public void bytesWritten(OutputFileEvent event) {}

      @Override
      public void transferEnded(OutputFileEvent event) {
        System.out.println("Downloaded " + (this.dl.get() / 1024 / 1024) + "MB of " + (this.total.get() / 1024 / 1024)
            + " MB");
      }
    };

    zsync.zsync(uri, options, l);
  }
}
