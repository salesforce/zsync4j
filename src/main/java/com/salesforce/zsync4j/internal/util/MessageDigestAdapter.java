package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.DigestException;
import java.security.MessageDigest;

/**
 * Turns a message digest into a channel, so it can be passed to method expecting a channel
 * 
 * @author bbusjaeger
 *
 */
public class MessageDigestAdapter implements WritableByteChannel {
  private final MessageDigest digest;

  public MessageDigestAdapter(MessageDigest digest) {
    this.digest = digest;
  }

  @Override
  public boolean isOpen() {
    return true;
  }

  @Override
  public void close() throws IOException {}

  @Override
  public int write(ByteBuffer src) throws IOException {
    final int r = src.remaining();
    digest.update(src);;
    return r;
  }

  public void reset() {
    digest.reset();
  }

  public int digest(byte[] buf, int offset, int len) throws DigestException {
    return digest.digest(buf, offset, len);
  }

}
