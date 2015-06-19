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
package com.salesforce.zsync4j.internal;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import com.google.common.collect.ImmutableList;

public class ImmutableBlockSum extends BlockSum {

  public static List<ImmutableBlockSum> readSums(InputStream in, int numBlocks, int rsumBytes, int checksumBytes)
      throws IOException {
    final ImmutableList.Builder<ImmutableBlockSum> b = ImmutableList.builder();
    for (int i = 0; i < numBlocks; i++) {
      b.add(ImmutableBlockSum.read(in, rsumBytes, checksumBytes));
    }
    return b.build();
  }

  public static ImmutableBlockSum read(InputStream in, int rsumBytes, int checksumBytes) throws IOException {
    return new ImmutableBlockSum(readRsum(in, rsumBytes), readChecksum(in, checksumBytes));
  }

  static int readRsum(InputStream in, int rsumBytes) throws IOException {
    int rsum = 0;
    for (int i = rsumBytes - 1; i >= 0; i--) {
      int next = in.read();
      if (next == -1) {
        throw new IllegalArgumentException("Failed to read rsum: premature end of file");
      }
      rsum |= next << (i * 8);
    }
    return rsum;
  }

  static byte[] readChecksum(InputStream in, int len) throws IOException {
    final byte[] b = new byte[len];
    int read = 0;
    int r;
    while (read < len && (r = in.read(b, read, len - read)) != -1) {
      read += r;
    }
    if (read != b.length) {
      throw new IOException("Failed to read block checksums");
    }
    return b;
  }


  private final int rsum;
  private final byte[] checksum;

  private volatile Integer hashCode;

  public ImmutableBlockSum(int rsum, byte[] checksum) {
    this.rsum = rsum;
    this.checksum = checksum;
  }

  @Override
  int getRsum() {
    return this.rsum;
  }

  @Override
  byte[] getChecksum() {
    return this.checksum;
  }

  @Override
  int getChecksumLength() {
    return this.checksum.length;
  }

  @Override
  public int hashCode() {
    if (this.hashCode == null) {
      this.hashCode = super.hashCode();
    }
    return this.hashCode;
  }
}
