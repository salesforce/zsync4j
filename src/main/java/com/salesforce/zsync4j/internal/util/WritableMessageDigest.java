package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.WritableByteChannel;
import java.security.MessageDigest;

/**
 * Turns a message digest into a channel, so it can be passed to method expecting a channel. The
 * behavior does not strictly comply with the Channel interface in that this adapter is not thread
 * safe and is always open, so it should really only be used internally.
 * 
 * @author bbusjaeger
 *
 */
public class WritableMessageDigest implements WritableByteChannel {

  private final MessageDigest messageDigest;

  public WritableMessageDigest(MessageDigest digest) {
    this.messageDigest = digest;
  }

  public MessageDigest getMessageDigest() {
    return this.messageDigest;
  }

  /**
   * Always open
   */
  @Override
  public boolean isOpen() {
    return true;
  }

  /**
   * Doesn't close anything
   */
  @Override
  public void close() throws IOException {}

  /**
   * Updates the digest with the given buffer. The returned bytes written is always equal to the
   * remaining bytes in the buffer per {@link MessageDigest#update(ByteBuffer)} spec.
   */
  @Override
  public int write(ByteBuffer src) throws IOException {
    final int r = src.remaining();
    this.messageDigest.update(src);
    return r;
  }

}
