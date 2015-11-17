/**
 * Copyright (c) 2015, Salesforce.com, Inc. All rights reserved.
 * 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met:
 * 
 * Redistributions of source code must retain the above copyright notice, this list of conditions
 * and the following disclaimer.
 * 
 * Redistributions in binary form must reproduce the above copyright notice, this list of conditions
 * and the following disclaimer in the documentation and/or other materials provided with the
 * distribution.
 * 
 * Neither the name of Salesforce.com nor the names of its contributors may be used to endorse or
 * promote products derived from this software without specific prior written permission.
 * 
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND ANY EXPRESS OR
 * IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
 * DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY,
 * WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY
 * WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.zsync.internal;

import static com.salesforce.zsync.Zsync.VERSION;
import static java.nio.charset.StandardCharsets.US_ASCII;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class Header {

  public static Header read(InputStream in) throws IOException {
    String version = null;
    String filename = null;
    Date mtime = null;
    Integer blocksize = null;
    Long length = null;
    // defaults for older versions of zsync
    int checksumBytes = 16;
    int rsumBytes = 4;
    boolean seqMatches = false;
    String url = null;
    String sha1 = null;

    boolean terminated = false;
    String line;
    BufferedReader reader = new BufferedReader(new InputStreamReader(in, US_ASCII));
    while ((line = reader.readLine()) != null) {
      if (line.length() == 0) {
        terminated = true;
        break;
      }
      final int index = line.indexOf(':');
      if (index == -1 || index == 0 || index >= line.length() - 2 || line.charAt(index + 1) != ' ') {
        throw new IllegalArgumentException("Invalid header line: " + line);
      }
      final String name = line.substring(0, index);
      final String value = line.substring(index + 2);

      // zsync doesn't check for duplicate headers
      if ("zsync".equals(name)) {
        if ("0.0.4".equals(value)) {
          throw new IllegalArgumentException("Incompatible zsync version " + value);
        }
        version = value;
      } else if ("Min-Version".equals(name)) {
        if (value.compareTo(VERSION) > 0) {
          throw new IllegalArgumentException("Zsync version " + VERSION + " does not satisfy min-version requirement "
              + value);
        }
      } else if ("Length".equals(name)) {
        try {
          length = Long.parseLong(value);
          if (length <= 0) {
            throw new NumberFormatException();
          }
        } catch (NumberFormatException e) {
          throwInvalidHeaderValue(name, value);
        }
      } else if ("Filename".equals(name)) {
        filename = value;
      } else if ("URL".equals(name)) {
        url = value;
      } else if ("Blocksize".equals(name)) {
        try {
          blocksize = Integer.parseInt(value);
          if (blocksize <= 0) {
            throw new NumberFormatException();
          }
        } catch (NumberFormatException e) {
          throwInvalidHeaderValue(name, value);
        }
      } else if ("Hash-Lengths".equals(name)) {
        try {
          final String[] split = value.split(",");
          if (split.length != 3) {
            throw new NumberFormatException();
          }
          final int sm = Integer.parseInt(split[0]);
          seqMatches = sm == 2;
          rsumBytes = Integer.parseInt(split[1]);
          checksumBytes = Integer.parseInt(split[2]);
          if (sm > 2 || sm < 1 || rsumBytes < 1 || rsumBytes > 4 || checksumBytes < 3 || checksumBytes > 16) {
            throw new NumberFormatException();
          }
        } catch (NumberFormatException e) {
          throwInvalidHeaderValue(name, value);
        }
      } else if ("SHA-1".equals(name)) {
        if (value.length() != 40) {
          throwInvalidHeaderValue(name, value);
        }
        sha1 = value;
      } else if ("MTime".equals(name)) {
        try {
          mtime = new SimpleDateFormat("EEE, dd MMMMM yyyy HH:mm:ss Z").parse(value);
        } catch (ParseException e) {
          throwInvalidHeaderValue(name, value);
        }
      } else {
        throw new IllegalArgumentException("Unsupported header " + line);
      }
    }

    if (!terminated) {
      throw new IllegalArgumentException("Invalid header: terminating line feed missing.");
    }
    if (filename == null) {
      throwMissingHeader("Filename");
    }
    if (blocksize == null) {
      throwMissingHeader("Blocksize");
    }
    if (length == null) {
      throwMissingHeader("Length");
    }
    if (length == null) {
      throwMissingHeader("URL");
    }
    if (sha1 == null) {
      throwMissingHeader("SHA-1");
    }
    return new Header(version, filename, mtime, blocksize, length, checksumBytes, rsumBytes, seqMatches, url, sha1);
  }

  private static void throwInvalidHeaderValue(String name, String value) {
    throw new IllegalArgumentException("Invalid " + name + " header value '" + value + "'");
  }

  private static void throwMissingHeader(String name) {
    throw new IllegalArgumentException("Missing header " + name);
  }

  private final String version;
  private final String filename;
  private final Date mtime;
  private final int blocksize;
  private final long length;
  private final int checksumBytes;
  private final int rsumBytes;
  private final boolean seqMatches;
  private final String url;
  private final String sha1;

  public Header(String version, String filename, Date mtime, int blocksize, long length, int checksumBytes,
      int rsumBytes, boolean seqMatches, String url, String sha1) {
    this.version = version;
    this.filename = filename;
    this.mtime = mtime;
    this.blocksize = blocksize;
    this.length = length;
    this.checksumBytes = checksumBytes;
    this.rsumBytes = rsumBytes;
    this.seqMatches = seqMatches;
    this.url = url;
    this.sha1 = sha1;
  }

  public String getVersion() {
    return this.version;
  }

  public String getFilename() {
    return this.filename;
  }

  public Date getMtime() {
    return this.mtime;
  }

  public int getBlocksize() {
    return this.blocksize;
  }

  public long getLength() {
    return this.length;
  }

  public int getChecksumBytes() {
    return this.checksumBytes;
  }

  public int getRsumBytes() {
    return this.rsumBytes;
  }

  public boolean isSeqMatches() {
    return this.seqMatches;
  }

  public String getUrl() {
    return this.url;
  }

  public String getSha1() {
    return this.sha1;
  }

  public int getNumBlocks() {
    return (int) ((this.length + this.blocksize - 1) / this.blocksize);
  }
}
