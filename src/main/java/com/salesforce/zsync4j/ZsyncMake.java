package com.salesforce.zsync4j;

import static com.salesforce.zsync4j.internal.util.ZsyncUtil.getFileSize;
import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.WritableByteChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;

import com.salesforce.zsync4j.internal.util.ZsyncUtil;

/**
 * Constructs a zsync file for a given input file. Construct via {@link ZsyncMakeTest.Builder}.
 * 
 * @author bbusjaeger
 */
public class ZsyncMake {

  public static String ZSYNC_VERSION = "0.6.2";

  private static final int BLOCK_SIZE_SMALL = 2048;
  private static final int BLOCK_SIZE_LARGE = 4096;

  @SuppressWarnings("serial")
  private static final SimpleDateFormat LAST_MODIFIED_TIME_FORMAT = new SimpleDateFormat(
      "EEE, dd MMMMM yyyy HH:mm:ss Z") {
    {
      setTimeZone(TimeZone.getTimeZone("GMT"));
    }
  };

  public ZsyncMake() {}

  /**
   * Writes zsync control data for the specified input file to the supplied output stream. The
   * output stream is not closed.
   * 
   * @param outputStream The stream to which zsync control data will be written.
   * @param inputFile Specifies the file for which the control data will be calculated.
   * @return The {@link Results results} of the zsyncmake operation.
   */
  public Result writeToStream(Path inputFile, OutputStream outputStream) {
    return this.writeToStream(inputFile, outputStream, new Options());
  }

  /**
   * Writes zsync control data for the specified input file to the supplied output stream using the
   * supplied {@link Options}. The output stream is not closed.
   * 
   * @param outputStream The stream to which zsync control data will be written.
   * @param inputFile Specifies the file for which the control data will be calculated.
   * @param options Advanced options for the zsyncmake operation.
   * @return The {@link Results results} of the zsyncmake operation.
   */
  public Result writeToStream(Path inputFile, OutputStream outputStream, Options options) {
    if (outputStream == null) {
      throw new IllegalArgumentException("outputStream cannot be null");
    }
    return writeToChannel(inputFile, Channels.newChannel(outputStream), options);
  }

  /**
   * Writes zsync control data for the specified input file to another file. The generated .zsync
   * file is in the same directory as the input file and is named the same as the input file but
   * with ".zsync" on the end.
   * 
   * @param inputFile Specifies the file for which the corresponding .zsync file will be written.
   * @return The {@link FileResult results} of the zsyncmake operation. The resulting .zsync file
   *         can be accessed via {@link FileResult#getOutputFile() results.getOutputFile()}.
   */
  public FileResult writeToFile(Path inputFile) {
    return this.writeToFile(inputFile, new Options());
  }

  /**
   * Writes zsync control data for the specified input file to the specified output file. The
   * filename of the output file must end with .zsync.
   * 
   * @param outputFile Specifies the file where the zsync control data will be written. If the file
   *        already exists it will be overwritten.
   * @param inputFile Specifies the file for which the corresponding .zsync file will be written.
   * @return The {@link FileResult results} of the zsyncmake operation. The resulting .zsync file
   *         can be accessed via {@link FileResult#getOutputFile() results.getOutputFile()}.
   */
  public FileResult writeToFile(Path inputFile, Path outputFile) {
    return this.writeToFile(inputFile, outputFile, new Options());
  }

  /**
   * Writes zsync control data for the specified input file to another file using the supplied
   * {@link Options}. The generated .zsync file is in the same directory as the input file and is
   * named the same as the input file but with ".zsync" on the end.
   * 
   * @param inputFile Specifies the file for which the corresponding .zsync file will be written.
   * @param options Advanced options for the zsyncmake operation.
   * @return The {@link FileResult results} of the zsyncmake operation. The resulting .zsync file
   *         can be accessed via {@link FileResult#getOutputFile() results.getOutputFile()}.
   */
  public FileResult writeToFile(Path inputFile, Options options) {
    if (inputFile == null) {
      throw new IllegalArgumentException("inputFile cannot be null");
    }
    return writeToFile(inputFile, inputFile.getParent().resolve(inputFile.getFileName() + ".zsync"), options);
  }

  /**
   * Writes zsync control data for the specified input file to the specified output file using the
   * supplied {@link Options}. The filename of the output file must end with .zsync.
   * 
   * @param outputFile Specifies the file where the zsync control data will be written. If the file
   *        already exists it will be overwritten.
   * @param inputFile Specifies the file for which the corresponding .zsync file will be written.
   * @param options Advanced options for the zsyncmake operation.
   * @return The {@link FileResult results} of the zsyncmake operation. The resulting .zsync file
   *         can be accessed via {@link FileResult#getOutputFile() results.getOutputFile()}.
   */
  public FileResult writeToFile(Path inputFile, Path outputFile, Options options) {
    if (outputFile == null) {
      throw new IllegalArgumentException("outputFile cannot be null");
    }
    if (!outputFile.getFileName().toString().endsWith(".zsync")) {
      throw new IllegalArgumentException("outputFile's filename must end with .zsync: "
          + outputFile.getFileName().toString());
    }
    try (FileOutputStream outputStream = new FileOutputStream(outputFile.toFile())) {
      Result result = this.writeToStream(inputFile, outputStream, options);
      return new FileResult(result.getSha1(), outputFile);
    } catch (IOException exception) {
      throw new RuntimeException("zsyncmake operation failed", exception);
    }
  }

  /*
   * Everything funnels into here.
   */
  public Result writeToChannel(Path inputFile, WritableByteChannel out, Options options) {

    if (inputFile == null) {
      throw new IllegalArgumentException("inputFile cannot be null");
    }
    if (!Files.exists(inputFile)) {
      throw new IllegalArgumentException("input file " + inputFile + " does not exist");
    }
    if (Files.isDirectory(inputFile)) {
      throw new IllegalArgumentException("input file " + inputFile + " is a directory");
    }
    if (options == null) {
      options = new Options();
    }

    final MessageDigest fileDigest = ZsyncUtil.newSHA1();
    final MessageDigest blockDigest = ZsyncUtil.newMD4();

    // We don't want to modify the Options object that was passed in, so we create a copy. We then
    // populate any missing
    // values using the supplied input file.
    options = new Options(options).calculateMissingValues(inputFile);

    final int blockSize = options.getBlockSize();
    final long fileLength = getFileSize(inputFile);
    final int sequenceMatches = fileLength > options.getBlockSize() ? 2 : 1;
    final int weakChecksumLength = weakChecksumLength(fileLength, blockSize, sequenceMatches);
    final int strongChecksumLength = strongChecksumLength(fileLength, blockSize, sequenceMatches);

    final ByteBuffer checksums =
        computeChecksums(inputFile, blockSize, fileLength, weakChecksumLength, strongChecksumLength, fileDigest,
            blockDigest);

    // first read sha1 from end of buffer
    final int pos = checksums.capacity() - fileDigest.getDigestLength();
    checksums.position(pos);
    final String sha1 = ZsyncUtil.toHexString(checksums);

    // set buffer to read from beginning to start of fileDigest
    checksums.clear().limit(pos);

    // first write headers
    writeHeader(out, "zsync", ZSYNC_VERSION);
    writeHeader(out, "Filename", options.getFilename());
    writeHeader(out, "MTime", getFormattedLastModifiedTime(inputFile));
    writeHeader(out, "Blocksize", String.valueOf(blockSize));
    writeHeader(out, "Length", String.valueOf(fileLength));
    writeHeader(out, "Hash-Lengths", sequenceMatches + "," + weakChecksumLength + "," + strongChecksumLength);
    writeHeader(out, "URL", options.getUrl());
    writeHeader(out, "SHA-1", sha1);
    writeHeader(out, "\n");

    try {
      do {
        out.write(checksums);
      } while (checksums.hasRemaining());
    } catch (IOException exception) {
      throw new RuntimeException("Failed to write checksums", exception);
    }

    return new Result(sha1);
  }

  private void writeHeader(WritableByteChannel out, String name, String value) {
    final String header =
        new StringBuilder(name.length() + value.length() + 3).append(name).append(": ").append(value).append('\n')
            .toString();
    writeHeader(out, header);
  }

  private void writeHeader(WritableByteChannel out, String header) {
    try {
      out.write(ByteBuffer.wrap(header.getBytes(US_ASCII)));
    } catch (IOException exception) {
      throw new RuntimeException("Unable to write header to zsync control file: " + header, exception);
    }
  }

  /**
   * Computes block- and file-level checksums for the inputFile according to the given weak and
   * strong checksum lengths. The returned buffer contains block-level checksum, each (weakLen +
   * strongLen) bytes in size, followed by the file-level checksum, which for SHA-1 is 20 bytes.
   * 
   * @param fileLength Length of the inputFile in bytes
   * @param weakLen Number of bytes to store for weak checksum in bytes
   * @param strongLen Number of bytes to store for strong checksum in bytes
   * @return byte buffer containing both block and file checksums. The block is returned ready for
   *         reading: position at 0 and limit at capacity.
   * @throws IOException
   */
  private ByteBuffer computeChecksums(final Path inputFile, final int blockSize, final long fileLength,
      final int weakLen, final int strongLen, MessageDigest fileDigest, MessageDigest blockDigest) {
    if (weakLen < 1 || weakLen > 4) {
      throw new IllegalArgumentException("weak checksum length must be in interval [1, 4]");
    }
    if (strongLen < 1 || strongLen > 16) {
      throw new IllegalArgumentException("strong checksum length must be in interval [1, 16]");
    }

    // capacity of buffer is number of blocks times checksum bytes per block
    final int capacity =
        ((int) (fileLength / blockSize) + (fileLength % blockSize > 0 ? 1 : 0)) * (weakLen + strongLen)
            + fileDigest.getDigestLength();

    // output buffer: may want to write to disk at certain size
    final ByteBuffer checksums = ByteBuffer.allocate(capacity);

    // buffer for converting weak checksum int to bytes
    final ByteBuffer weakBytes = ByteBuffer.allocate(4);

    // buffer for each block read from input file
    final byte[] block = new byte[blockSize];

    try {
      // wrap file input stream with digest input stream to compute SHA-1 while reading file
      try (final InputStream in = new DigestInputStream(Files.newInputStream(inputFile), fileDigest)) {
        int read;
        while ((read = in.read(block)) != -1) {
          // pad last block with 0s
          if (read < blockSize)
            Arrays.fill(block, read, blockSize, (byte) 0);

          // write trailing bytes of weak checksum
          weakBytes.clear();
          weakBytes.putInt(ZsyncUtil.computeRsum(block));
          weakBytes.position(weakBytes.limit() - weakLen);
          checksums.put(weakBytes);

          // write leading bytes of strong checksum
          final ByteBuffer strongBytes = ByteBuffer.wrap(blockDigest.digest(block));
          strongBytes.limit(strongLen);
          checksums.put(strongBytes);
        }
      }
    } catch (IOException exception) {
      throw new RuntimeException("Failed calculating zsync checksum", exception);
    }

    // finally add file checksum
    final ByteBuffer checksum = ByteBuffer.wrap(fileDigest.digest());
    checksums.put(checksum);

    // flip to allow reading from buffer
    checksums.flip();

    return checksums;
  }

  /**
   * Used to supply advanced options to the zsyncmake operation.
   * <p>
   * Usage:
   * 
   * <pre>
   * Options options = new Options();
   * options.setBlockSize(desiredBlockSize);
   * options.setFilename(&quot;thefile.dat&quot;);
   * Path outputFile = new ZsyncMake().writeToFile(inputFile, options).getOutputFile();
   * </pre>
   */
  public static class Options {

    private Integer blockSize;
    private String filename;
    private String url;

    public Options() {}

    public Options(Options other) {
      this.blockSize = other.getBlockSize();
      this.filename = other.getFilename();
      this.url = other.getUrl();
    }

    public Integer getBlockSize() {
      return this.blockSize;
    }

    public Options setBlockSize(Integer blockSize) {
      if (blockSize != null && blockSize < 0) {
        throw new IllegalArgumentException("blockSize must be greater than zero: " + blockSize);
      }
      if (blockSize != null && ((blockSize & (blockSize - 1)) != 0)) {
        throw new IllegalArgumentException("blockSize must be a power of 2");
      }
      this.blockSize = blockSize;
      return this;
    }

    public String getFilename() {
      return this.filename;
    }

    public Options setFilename(String filename) {
      this.filename = filename;
      return this;
    }

    public String getUrl() {
      return this.url;
    }

    public Options setUrl(String url) {
      if (url != null) {
        try {
          new URI(url);
        } catch (URISyntaxException exception) {
          throw new IllegalArgumentException("Invalid URL " + url, exception);
        }
      }
      this.url = url;
      return this;
    }

    /**
     * Resolves option values which are required for the zsyncmake operation but which were not
     * supplied.
     */
    private Options calculateMissingValues(Path inputFile) {
      // blocksize: default chosen based on file size (adopted from standard zsync implementation)
      if (this.blockSize == null) {
        this.blockSize = calculateDefaultBlockSizeForInputFile(inputFile);
      }

      // filename: default from inputFile
      if (this.filename == null) {
        this.filename = inputFile.getFileName().toString();
      }

      // url: default to filename relative URL
      if (this.url == null) {
        this.setUrl(this.filename);
      }

      return this;
    }
  }

  /**
   * The results of a zsyncmake operation. As part of the zsyncmake operation, a SHA-1 hash of the
   * input file is calculated and can be accessed via <code>result.getSha1()</code>.
   */
  public static class Result {

    private final String sha1;

    private Result(String sha1) {
      this.sha1 = sha1;
    }

    public String getSha1() {
      return this.sha1;
    }
  }

  /**
   * The results of a <code>writeToFile(...)</code> zsyncmake operation. The <code>.zsync</code>
   * file that was created can be accessed via <code>results.getOutputFile()</code>.
   */
  public static class FileResult extends Result {

    private final Path outputFile;

    private FileResult(String sha1, Path outputFile) {
      super(sha1);
      this.outputFile = outputFile;
    }

    public Path getOutputFile() {
      return this.outputFile;
    }
  }

  /**
   * Creates a zsync control file for the specified input file in the same directory as the input
   * file.
   * <p>
   * Usage:
   * 
   * <pre>
   * java -classpath &lt;path-to-zsync4j-jar&gt; com.salesforce.zsync4j.ZsyncMake &lt;path-to-input-file&gt;
   * </pre>
   */
  public static void main(String[] args) {
    final Path inputFile = FileSystems.getDefault().getPath(args[0]);
    new ZsyncMake().writeToFile(inputFile);
  }

  /**
   * Computes how many bytes to store for the strong checksum.
   * 
   * @param fileLength
   * @param blocksize
   * @param sequenceMatches
   * @return integer in range [3, 16]
   */
  static int strongChecksumLength(long fileLength, int blocksize, int sequenceMatches) {
    // estimated number of bytes to allocate for strong checksum
    final double d = (Math.log(fileLength) + Math.log(1 + fileLength / blocksize)) / Math.log(2) + 20;

    // reduced number of bits by sequence matches
    final int l1 = (int) Math.ceil(d / sequenceMatches / 8);

    // second checksum - not reduced by sequence matches
    final int l2 = (int) ((Math.log(1 + fileLength / blocksize) / Math.log(2) + 20 + 7.9) / 8);

    // return max of two: return no more than 16 bytes (MD4 max)
    return Math.min(16, Math.max(l1, l2));
  }

  /**
   * Computes how many bytes to store for the weak checksum. The formula is derived by minimizing
   * estimated download time. See http://zsync.moria.org.uk/paper/ch02s03.html.
   * 
   * @param fileLength
   * @param blocksize
   * @param sequenceMatches
   * @return integer in range [2,4]
   */
  static int weakChecksumLength(long fileLength, int blocksize, int sequenceMatches) {
    // estimated number of bytes to allocate for the rolling checksum per formula in
    // Weak Checksum section of http://zsync.moria.org.uk/paper/ch02s03.html
    final double d = (Math.log(fileLength) + Math.log(blocksize)) / Math.log(2) - 8.6;

    // reduced number of bits by sequence matches per http://zsync.moria.org.uk/paper/ch02s04.html
    final int l = (int) Math.ceil(d / sequenceMatches / 8);

    // enforce max and min values
    return l > 4 ? 4 : (l < 2 ? 2 : l);
  }

  private static int calculateDefaultBlockSizeForInputFile(Path inputFile) {
    try {
      return Files.size(inputFile) < 100 * 1 << 20 ? BLOCK_SIZE_SMALL : BLOCK_SIZE_LARGE;
    } catch (IOException exception) {
      throw new RuntimeException("Error calculating the default block size for file: " + inputFile.getFileName(),
          exception);
    }
  }

  /**
   * Returns the last modified time of the supplied file, formatted like
   * "Sun, 03 May 2015 19:12:19 -0800".
   */
  private static String getFormattedLastModifiedTime(Path file) {
    try {
      long lastModifiedTime = Files.readAttributes(file, BasicFileAttributes.class).lastModifiedTime().toMillis();
      return LAST_MODIFIED_TIME_FORMAT.format(new Date(lastModifiedTime));
    } catch (IOException exception) {
      throw new RuntimeException("Could not read last modified time from file: " + file.getFileName(), exception);
    }
  }
}
