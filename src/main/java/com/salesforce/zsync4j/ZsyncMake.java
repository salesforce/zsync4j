package com.salesforce.zsync4j;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.TimeZone;
import java.util.concurrent.Callable;

import com.salesforce.zsync4j.internal.util.MD4Provider;

/**
 * Constructs a zsync file for a given input file. Construct via {@link ZsyncMakeTest.Builder}.
 * 
 * @author bbusjaeger
 */
public class ZsyncMake implements Callable<Path> {

  public static final String VERSION = "0.6.2";

  private final Path inputFile;
  private final Path outputFile;
  private final String filename;
  private final URI url;
  private final int blocksize;

  // currently not configurable as these are fixed for zsync
  private final MessageDigest fileDigest;
  private final MessageDigest blockDigest;
  private final DateFormat mtimeFormat;

  private ZsyncMake(Path inputFile, Path outputFile, String filename, URI url, int blocksize) {
    this.inputFile = inputFile;
    this.outputFile = outputFile;
    this.filename = filename;
    this.url = url;
    this.blocksize = blocksize;

    try {
      this.blockDigest = MessageDigest.getInstance("MD4", new MD4Provider());
      this.fileDigest = MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("Failed to construct ZsyncMake because required digest unavailable", e);
    }
    this.mtimeFormat = new SimpleDateFormat("EEE, dd MMMMM yyyy HH:mm:ss Z");
    this.mtimeFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
  }

  /**
   * Returns input file from which to construct the zsync file
   * 
   * @return
   */
  public Path getInputFile() {
    return inputFile;
  }

  /**
   * Returns output file where zsync file will be written
   * 
   * @return
   */
  public Path getOutputFile() {
    return outputFile;
  }

  /**
   * Returns filename as it will be included in zsync file header
   * 
   * @return
   */
  public String getFilename() {
    return filename;
  }

  /**
   * Returns url of content file as it will be included in zsync file header
   *
   * @return
   */
  public URI getUrl() {
    return url;
  }

  /**
   * Returns block size used to compute checksums and to include in zsync file header
   * 
   * @return
   */
  public int getBlocksize() {
    return blocksize;
  }

  public Path call() throws IOException {
    final long fileLength = Files.size(inputFile);
    final int sequenceMatches = fileLength > blocksize ? 2 : 1;
    final int weakChecksumLength = weakChecksumLength(fileLength, blocksize, sequenceMatches);
    final int strongChecksumLength = strongChecksumLength(fileLength, blocksize, sequenceMatches);

    final ByteBuffer checksums = computeChecksums(fileLength, weakChecksumLength, strongChecksumLength);

    // first read sha1 from end of buffer
    final int pos = checksums.capacity() - fileDigest.getDigestLength();
    checksums.position(pos);
    final String sha1 = toHexString(checksums);

    // set buffer to read from beginning to start of fileDigest
    checksums.clear().limit(pos);

    try (FileChannel fc = FileChannel.open(outputFile, CREATE, WRITE)) {
      // first write header
      writeHeader(fc, "zsync", VERSION);
      writeHeader(fc, "Filename", filename);
      writeHeader(fc, "MTime", getMtime());
      writeHeader(fc, "Blocksize", String.valueOf(blocksize));
      writeHeader(fc, "Length", String.valueOf(fileLength));
      writeHeader(fc, "Hash-Lengths", sequenceMatches + "," + weakChecksumLength + "," + strongChecksumLength);
      writeHeader(fc, "URL", url.toString());
      writeHeader(fc, "SHA-1", sha1);
      writeHeader(fc, "\n");

      do {
        fc.write(checksums);
      } while (checksums.hasRemaining());
    }

    return outputFile;
  }

  private void writeHeader(FileChannel fc, String name, String value) throws IOException {
    final String header = new StringBuilder(name.length() + value.length() + 3).append(name).append(": ").append(value).append('\n').toString();
    writeHeader(fc, header);
  }

  private void writeHeader(FileChannel fc, String header) throws IOException {
    fc.write(ByteBuffer.wrap(header.getBytes(US_ASCII)));
  }

  private String getMtime() throws IOException {
    final long mtime = Files.readAttributes(inputFile, BasicFileAttributes.class).lastModifiedTime().toMillis();
    return mtimeFormat.format(new Date(mtime));
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
  private ByteBuffer computeChecksums(final long fileLength, final int weakLen, final int strongLen) throws IOException {
    if (weakLen < 1 || weakLen > 4)
      throw new IllegalArgumentException("weak checksum length must be in interval [1, 4]");
    if (strongLen < 1 || strongLen > 16)
      throw new IllegalArgumentException("strong checksum length must be in interval [1, 16]");

    // capacity of buffer is number of blocks times checksum bytes per block
    final int capacity = ((int) (fileLength / blocksize) + (fileLength % blocksize > 0 ? 1 : 0)) * (weakLen + strongLen) + fileDigest.getDigestLength();

    // output buffer: may want to write to disk at certain size
    final ByteBuffer checksums = ByteBuffer.allocate(capacity);

    // buffer for converting weak checksum int to bytes
    final ByteBuffer weakBytes = ByteBuffer.allocate(4);

    // buffer for each block read from input file
    final byte[] block = new byte[blocksize];

    // wrap file input stream with digest input stream to compute SHA-1 while reading file
    try (final InputStream in = new DigestInputStream(Files.newInputStream(inputFile), fileDigest)) {
      int read;
      while ((read = in.read(block)) != -1) {
        // pad last block with 0s
        if (read < blocksize)
          Arrays.fill(block, read, blocksize, (byte) 0);

        // write trailing bytes of weak checksum
        weakBytes.clear();
        weakBytes.putInt(weakChecksum(block));
        weakBytes.position(weakBytes.limit() - weakLen);
        checksums.put(weakBytes);

        // write leading bytes of strong checksum
        final ByteBuffer strongBytes = ByteBuffer.wrap(blockDigest.digest(block));
        strongBytes.limit(strongLen);
        checksums.put(strongBytes);
      }
    }

    // finally add file checksum
    final ByteBuffer checksum = ByteBuffer.wrap(fileDigest.digest());
    checksums.put(checksum);

    // flip to allow reading from buffer
    checksums.flip();

    return checksums;
  }

  static int weakChecksum(byte[] block) {
    short a = 0;
    short b = 0;
    for (int i = 0, l = block.length; i < block.length; i++, l--) {
      final short val = unsigned(block[i]);
      a += val;
      b += l * val;
    }
    return (a << 16) | (b & 0xffff);
  }

  static short unsigned(byte b) {
    return (short) (b < 0 ? b & 0xFF : b);
  }

  private static final char[] hexCode = "0123456789abcdef".toCharArray();

  static String toHexString(ByteBuffer buffer) {
    final StringBuilder r = new StringBuilder(buffer.remaining() * 2);
    while (buffer.hasRemaining()) {
      final byte b = buffer.get();
      r.append(hexCode[(b >> 4) & 0xF]);
      r.append(hexCode[(b & 0xF)]);
    }
    return r.toString();
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
   * Checks and defaults parameters to {@link ZsyncMakeTest}.
   *
   * @author bbusjaeger
   */
  public static class Builder {

    private static final int BLOCK_SIZE_SMALL = 2048;
    private static final int BLOCK_SIZE_LARGE = 4096;

    // required parameters
    private final Path inputFile;

    // optional parameters
    private Path outputFile;
    private String filename;
    private String url;
    private Integer blocksize;

    /**
     * Constructs a builder for the given inputFile
     * 
     * @param inputFile
     */
    public Builder(Path inputFile) {
      if (inputFile == null)
        throw new NullPointerException();
      this.inputFile = inputFile;
    }

    /**
     * Sets the location to which the zsync file will be emitted. Defaults to
     * <code>inputFile + ".zsync"</code>.
     * 
     * @param path
     * @return
     */
    public Builder setOutputFile(Path outputFile) {
      this.outputFile = outputFile;
      return this;
    }

    /**
     * Value of the zsync <code>Filename</code> header used by clients to name the file constructed
     * by running the zsync algorithm locally. If not specified, defaults to the last path segment
     * of the inputFile.
     *
     * @param filename
     * @return
     */
    public Builder setFilename(String filename) {
      this.filename = filename;
      return this;
    }

    /**
     * Specifies the url from which the content described by the zsync file can be downloaded. The
     * given input string is parsed into a URL per {@link URI#URI(String)}. If not specified, it
     * defaults to the filename, i.e. it is assumed that the zsync file resides in the same public
     * directory as the content file and it can be retrieved via the relative URL.
     *
     * @param url
     * @return
     */
    public Builder setUrl(String url) {
      this.url = url;
      return this;
    }

    /**
     * Size of blocks in bytes to use for constructing the zsync file. Must be a positive integer
     * and power of 2. If not specified, best default for file is chosen (currently either 2048 or
     * 4096 depending on file size), so normally you should not need to override the default.
     * <p>
     * The underlying rsync algorithm splits the input file into blocks of the given size and
     * calculates checksums for each block. The blocksize is also included in the zsync file header
     * <code>Blocksize</code> and used by clients to find matching blocks. Therefore, a smaller
     * blocksize may be more efficient for files where there are likely to be lots of small,
     * scattered changes between downloads. A larger blocksize is more efficient for files with
     * fewer or less scattered changes.
     *
     * @param blocksize
     * @return
     */
    public Builder setBlocksize(int blocksize) {
      this.blocksize = blocksize;
      return this;
    }

    public ZsyncMake build() throws IOException {
      // validate and default inputs
      if (!Files.exists(inputFile))
        throw new IllegalArgumentException("input file " + inputFile + " does not exist");
      if (Files.isDirectory(inputFile))
        throw new IllegalArgumentException("input file " + inputFile + " is a directory");

      // blocksize: default chosen based on file size (adopted from standard zsync implementation)
      final int b;
      if (blocksize == null) {
        b = Files.size(inputFile) < 100 * 1 << 20 ? BLOCK_SIZE_SMALL : BLOCK_SIZE_LARGE;
      } else {
        if (blocksize < 0)
          throw new IllegalArgumentException("blocksize must be a positive integer");
        if ((blocksize & (blocksize - 1)) != 0)
          throw new IllegalArgumentException("blocksize must be power of 2");
        b = blocksize;
      }

      // filename: default from inputFile
      final String f = filename == null ? inputFile.getFileName().toString() : filename;

      // url: default to filename relative URL
      final URI u;
      try {
        u = new URI(url == null ? f : url);
      } catch (URISyntaxException e) {
        throw new IllegalArgumentException("Invalid url " + e.getMessage());
      }

      // outputfile: defaults to inputFile + '.zsync'
      final Path o = outputFile == null ? inputFile.getParent().resolve(inputFile.getFileName() + ".zsync") : outputFile;

      return new ZsyncMake(inputFile, o, f, u, b);
    }

  }

  public static void main(String[] args) throws IOException {
    final Path inputFile = FileSystems.getDefault().getPath(args[0]);
    final ZsyncMake make = new Builder(inputFile).build();
    make.call();
  }

}
