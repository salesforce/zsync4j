package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.Provider;

import org.apache.mina.proxy.utils.MD4;

public class ZsyncUtil {

  private static final char[] HEX_CODE = "0123456789abcdef".toCharArray();
  private static final Provider md4Provider;

  static {
    md4Provider = new Provider("MD4Provider", 1d, "implements md4") {
      private static final long serialVersionUID = 6386613936557154160L;
    };
    md4Provider.put("MessageDigest.MD4", MD4.class.getName());
  }

  public static String toHexString(ByteBuffer buffer) {
    final StringBuilder r = new StringBuilder(buffer.remaining() * 2);
    while (buffer.hasRemaining()) {
      final byte b = buffer.get();
      r.append(HEX_CODE[(b >> 4) & 0xF]);
      r.append(HEX_CODE[(b & 0xF)]);
    }
    return r.toString();
  }

  public static int computeRsum(byte[] block) {
    short a = 0;
    short b = 0;
    for (int i = 0, l = block.length; i < block.length; i++, l--) {
      final short val = unsigned(block[i]);
      a += val;
      b += l * val;
    }
    return toInt(a, b);
  }

  public static int toInt(short x, short y) {
    return (x << 16) | (y & 0xffff);
  }

  public static long toLong(int x, int y) {
    return (((long) x) << 32) | (y & 0xffffffffL);
  }

  private static short unsigned(byte b) {
    return (short) (b < 0 ? b & 0xFF : b);
  }

  public static String computeSha1(Path path) throws IOException {
    final MessageDigest sha1 = newSHA1();
    final byte[] b = new byte[8192];
    try (InputStream in = Files.newInputStream(path)) {
      int r;
      while ((r = in.read(b)) != -1) {
        sha1.update(b, 0, r);
      }
    }
    return toHexString(ByteBuffer.wrap(sha1.digest()));
  }

  public static MessageDigest newMD4() {
    try {
      return MessageDigest.getInstance("MD4", md4Provider);
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("MD4 unavailable");
    }
  }

  public static MessageDigest newSHA1() {
    try {
      return MessageDigest.getInstance("SHA-1");
    } catch (NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-1");
    }
  }

}
