package com.salesforce.zsync4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.Proxy;
import java.net.URI;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.salesforce.zsync4j.Zsync.Options.Credentials;
import com.salesforce.zsync4j.internal.BlockMatcher;
import com.salesforce.zsync4j.internal.ControlFile;
import com.salesforce.zsync4j.internal.EventManagerImpl;
import com.salesforce.zsync4j.internal.Header;
import com.salesforce.zsync4j.internal.OutputFile;
import com.salesforce.zsync4j.internal.util.HttpClient;
import com.salesforce.zsync4j.internal.util.RollingBuffer;
import com.salesforce.zsync4j.internal.util.ZeroPaddedReadableByteChannel;
import com.salesforce.zsync4j.internal.util.ZsyncUtil;
import com.squareup.okhttp.Authenticator;
import com.squareup.okhttp.OkHttpClient;
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

  private final OkHttpClient okHttpClient;
  private final EventManagerImpl events;

  public Zsync() {
    this(new OkHttpClient());
  }

  public Zsync(OkHttpClient okHttpClient) {
    this.okHttpClient = okHttpClient;
    this.events = new EventManagerImpl();
  }

  public void zsync(URI zsyncFile, Options options) throws ZsyncFileNotFoundException, OutputFileValidationException {
    try {
      options = new Options(options); // Copy, since the supplied Options is mutable
      this.events.transferStarted(zsyncFile, options);
      this.zsyncInternal(zsyncFile, options);
      this.events.transferComplete();
    } catch (ZsyncFileNotFoundException exception) {
      this.failAndRethrow(exception);
    } catch (OutputFileValidationException exception) {
      this.failAndRethrow(exception);
    } catch (RuntimeException exception) {
      this.failAndRethrow(exception);
    }
  }

  private <T extends Exception> void failAndRethrow(T exception) throws T {
    this.events.transferFailed(exception);
    throw exception;
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
  private void zsyncInternal(URI zsyncFile, Options options) throws ZsyncFileNotFoundException,
      OutputFileValidationException {

    final HttpClient httpClient = this.createHttpClient(options.getCredentials());

    final ControlFile controlFile;
    this.events.controlFileProcessingStarted(zsyncFile);
    try (InputStream in = this.openZsyncFile(zsyncFile, httpClient, options)) {
      controlFile = ControlFile.read(in, this.events);
    } catch (FileNotFoundException e) {
      throw new ZsyncFileNotFoundException("Zsync file " + zsyncFile + " does not exist.", e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read zsync control file", e);
    }

    // determine output file location
    Path outputFile = options.getOutputFile();
    if (outputFile == null) {
      outputFile = Paths.get(controlFile.getHeader().getFilename());
    }
    this.events.outputFileResolved(outputFile);

    // determine remote file location
    URI remoteFileUri = URI.create(controlFile.getHeader().getUrl());
    if (!remoteFileUri.isAbsolute()) {
      remoteFileUri = options.getZsyncFileSource().resolve(remoteFileUri);
    }

    try (final OutputFile targetFile = new OutputFile(outputFile, controlFile, this.events)) {
      boolean outputFileComplete = this.processInputFiles(targetFile, controlFile, options.getInputFiles());
      if (!outputFileComplete) {
        httpClient.partialGet(remoteFileUri, targetFile.getMissingRanges(), targetFile, this.events, this.events);
      }
    } catch (IOException exception) {
      throw new RuntimeException(exception);
    }
  }

  /**
   * Opens the zsync file referred to by the given URI for read. If the file refers to a local file
   * system path, the local file is opened directly. Otherwise, if the file is remote and
   * {@link Options#getSaveZsyncFile()} is specified, the remote file is stored locally in the given
   * location first and then opened for read locally. If the file is remote and no save location is
   * specified, the file is opened for read over the remote connection.
   * <p>
   * If the file is remote, the method always calls {@link Options#setZsyncFileSource(URI)} on the
   * passed in options parameter, so that relative file URLs in the control file can later be
   * resolved against it.
   *
   * @param zsyncFile
   * @param httpClient
   * @param options
   *
   * @return
   * @throws IOException
   */
  private InputStream openZsyncFile(URI zsyncFile, HttpClient httpClient, Options options) throws IOException {
    final InputStream in;
    if (zsyncFile.isAbsolute()) {
      // check if it's a local URI
      final Path path = ZsyncUtil.getPath(zsyncFile);
      if (path == null) {
        // TODO we may want to set the redirect URL resulting from processing the http request
        options.setZsyncFileSource(zsyncFile);
        // check if we should persist the file locally
        final Path savePath = options.getSaveZsyncFile();
        if (savePath == null) {
          return httpClient.get(zsyncFile, null);
        } else {
          return httpClient.get(zsyncFile, null, savePath);
        }
      } else {
        in = Files.newInputStream(path);
      }
    } else {
      final String path = zsyncFile.getPath();
      if (path == null) {
        throw new IllegalArgumentException("Invalid zsync file URI: path of relative URI missing");
      }
      in = Files.newInputStream(Paths.get(path));
    }
    return in;
  }

  /**
   * Creates an HTTP client configured with the given credentials map. Uses a shallow copy of the
   * OkHttpClient to not modify the original copy per <a
   * href="https://github.com/square/okhttp/wiki/Recipes#per-call-configuration">Per-call
   * Configuration</a>
   *
   * @param credentials
   * @return
   */
  HttpClient createHttpClient(final Map<String, Credentials> credentials) {
    final OkHttpClient clone = this.okHttpClient.clone();
    clone.setAuthenticator(new Authenticator() {
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
    return new HttpClient(clone);
  }

  private boolean processInputFiles(OutputFile targetFile, ControlFile controlFile, Iterable<? extends Path> inputFiles)
      throws IOException {
    for (Path inputFile : inputFiles) {
      if (this.processInputFile(targetFile, controlFile, inputFile)) {
        return true;
      }
    }
    return false;
  }

  private boolean processInputFile(OutputFile targetFile, ControlFile controlFile, Path inputFile) throws IOException {
    this.events.inputFileProcessingStarted(inputFile);
    try (final FileChannel channel = FileChannel.open(inputFile)) {
      final BlockMatcher matcher = BlockMatcher.create(controlFile);
      final ReadableByteChannel c = zeroPad(channel, controlFile.getHeader());
      final RollingBuffer buffer = new RollingBuffer(c, matcher.getMatchBytes(), 16 * matcher.getMatchBytes());
      int bytes;
      do {
        bytes = matcher.match(targetFile, buffer);
      } while (buffer.advance(bytes));
    }
    this.events.inputFileProcessingComplete(inputFile);
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
    /*
     * final OutputFileListener l = new OutputFileListener() { final AtomicLong total = new
     * AtomicLong(); final AtomicLong dl = new AtomicLong();
     *
     * @Override public void transferStarted(OutputFileEvent event) {
     * this.total.set(event.getRemoteFileSizeInBytes()); }
     *
     * @Override public void bytesDownloaded(OutputFileEvent event) {
     * this.dl.addAndGet(event.getBytesDownloaded()); }
     *
     * @Override public void bytesWritten(OutputFileEvent event) {}
     *
     * @Override public void transferEnded(OutputFileEvent event) { System.out.println("Downloaded "
     * + (this.dl.get() / 1024 / 1024) + "MB of " + (this.total.get() / 1024 / 1024) + " MB"); } };
     */

    zsync.zsync(uri, options);
  }
}
