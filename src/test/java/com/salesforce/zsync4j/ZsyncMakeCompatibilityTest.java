package com.salesforce.zsync4j;

import static java.nio.charset.StandardCharsets.US_ASCII;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.Test;

import com.salesforce.zsync4j.ZsyncMake.Options;

/**
 * Exercises {@link ZsyncMake} against zsync files generated with official zsync version 0.6.2
 *
 * @author bbusjaeger
 */
public class ZsyncMakeCompatibilityTest {

  private final String name = "zsync_doc.pdf";

  @Test
  public void test() throws IOException {
    final Path inputFile = createTempInputFile(name);
    final Path expected = createTempInputFile(name + ".zsync");
    final Path actual = new ZsyncMake().writeToFile(inputFile, new Options().setFilename(name)).getOutputFile();
    try {
      assertArrayEquals("Generated zsync file does not match expected file", readWithoutMTime(expected), readWithoutMTime(actual));
    } finally {
      Files.deleteIfExists(actual);
      Files.deleteIfExists(expected);
      Files.deleteIfExists(actual);
    }
  }

  private static Path createTempInputFile(String name) throws IOException {
    try (final InputStream in = ZsyncMakeCompatibilityTest.class.getResourceAsStream(name);) {
      assertNotNull("Test precondition violated: test file " + name + " could not be found", in);
      final Path tmp = Files.createTempFile(name, null);
      Files.copy(in, tmp, REPLACE_EXISTING);
      return tmp;
    }
  }

  /**
   * Strips MTime header, because (1) zsyncmake seems to generate it incorrectly and (2) to avoid
   * having this test case rely on file system timestamps.
   * 
   * @param path
   * @return
   * @throws IOException
   */
  private static byte[] readWithoutMTime(Path path) throws IOException {
    final byte[] mtime = "MTime: ".getBytes(US_ASCII);
    final byte[] blocksize = "Blocksize: ".getBytes(US_ASCII);
    final byte[] bytes = Files.readAllBytes(path);
    int start = -1;
    int end = -1;
    for (int i = 0; i < bytes.length; i++) {
      if (start == -1) {
        if (matches(bytes, i, mtime))
          start = i;
      } else if (end == -1) {
        if (matches(bytes, i, blocksize)) {
          end = i;
          break;
        }
      }
    }
    final byte[] stripped = new byte[bytes.length - (end - start)];
    for (int i = 0, j = 0; i < bytes.length; i++) {
      if (i < start || i >= end)
        stripped[j++] = bytes[i];
    }
    return stripped;
  }

  private static boolean matches(byte[] file, int offset, byte[] content) {
    for (int i = 0, j = offset; i < content.length; i++, j++) {
      if (file[j] != content[i])
        return false;
    }
    return true;
  }

}
