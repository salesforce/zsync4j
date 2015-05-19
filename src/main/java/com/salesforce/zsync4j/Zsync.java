package com.salesforce.zsync4j;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLStreamHandler;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.google.common.base.Stopwatch;
import com.salesforce.zsync4j.internal.BlockMatcher;
import com.salesforce.zsync4j.internal.ControlFile;
import com.salesforce.zsync4j.internal.Range;
import com.salesforce.zsync4j.internal.TargetFile;
import com.salesforce.zsync4j.internal.util.RangeFetcher;
import com.salesforce.zsync4j.internal.util.RollingBuffer;
import com.squareup.okhttp.OkHttpClient;
import com.squareup.okhttp.OkUrlFactory;

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
        return username;
      }

      /**
       * Password for remote authentication
       * 
       * @return
       */
      public String getPassword() {
        return password;
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
      return outputFile;
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
      return saveZsyncFile;
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
      return zsyncUri;
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
      return credentials;
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
   * @throws ZsyncFileNotFoundException
   * @throws OutputFileValidationException
   */
  public void zsync(URI zsyncFile, Options options) throws ZsyncFileNotFoundException, OutputFileValidationException {
    final Stopwatch s = Stopwatch.createStarted();

    // create copy, since options mutable
    options = new Options(options);

    final ControlFile controlFile;
    try (InputStream in = toURL(zsyncFile).openStream()) {
      controlFile = ControlFile.read(in);
    } catch (FileNotFoundException e) {
      throw new ZsyncFileNotFoundException("Zsync file " + zsyncFile + " does not exist.", e);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read zsync control file", e);
    }

    final Path outputFile = options.getOutputFile() == null ? FileSystems.getDefault().getPath(controlFile.getHeader().getFilename()) : options.getOutputFile();
    final List<Path> inputFiles = options.getInputFiles();
    if (inputFiles.isEmpty())
      throw new UnsupportedOperationException("TODO implement");

    try (final TargetFile targetFile = new TargetFile(outputFile, controlFile)) {
      if (!processInputFiles(targetFile, controlFile, inputFiles))
        fetchRanges(targetFile, zsyncFile.resolve(controlFile.getHeader().getUrl()));
    } catch (OutputFileValidationException e) {
      throw e;
    } catch (IOException e) {
      throw new RuntimeException("Failed to write target file file", e);
    }

    System.out.println(s.stop());
  }

  static boolean processInputFiles(TargetFile targetFile, ControlFile controlFile, Iterable<? extends Path> inputFiles) throws IOException {
    for (Path inputFile : inputFiles)
      if (processInputFile(targetFile, controlFile, inputFile))
        return true;
    return false;
  }

  static boolean processInputFile(TargetFile targetFile, ControlFile controlFile, Path inputFile) throws IOException {
    // TODO pad end of input file
    try (final FileChannel channel = FileChannel.open(inputFile)) {
      final BlockMatcher matcher = BlockMatcher.create(controlFile);
      final RollingBuffer buffer = new RollingBuffer(channel, matcher.getMatchBytes(), 16 * matcher.getMatchBytes());
      int bytes;
      do {
        bytes = matcher.match(targetFile, buffer);
      } while (buffer.advance(bytes));
    }
    return targetFile.isComplete();
  }

  void fetchRanges(TargetFile targetFile, URI url) {
    final List<Range> missingRanges = targetFile.getMissingRanges();
    if (missingRanges.isEmpty())
      return;
    System.out.println("Missing ranges: " + missingRanges);
    final RangeFetcher fetcher = new RangeFetcher(httpUrlFactory.client());
    fetcher.fetch(url, missingRanges, targetFile);
  }

  private URL toURL(URI uri) throws MalformedURLException {
    final URLStreamHandler handler = httpUrlFactory.createURLStreamHandler(uri.getScheme());
    return handler == null ? uri.toURL() : new URL(uri.getScheme(), uri.getHost(), uri.getPort(), uri.getPath(), handler);
  }

  public static void main(String[] args) throws IOException, ZsyncFileNotFoundException {
    if (args.length != 3)
      throw new IllegalArgumentException("wrong number of args");
    final URI uri = URI.create(args[0]);
    final FileSystem fs = FileSystems.getDefault();
    final Options options = new Options().addInputFile(fs.getPath(args[1])).setOutputFile(fs.getPath(args[2]));
    final Zsync zsync = new Zsync(new OkHttpClient());
     for (int i = 0; i < 10; i++)
    zsync.zsync(uri, options);
  }

}
