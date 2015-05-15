package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.channels.WritableByteChannel;

/**
 * Read-only view onto a byte buffer
 * 
 * @author bbusjaeger
 *
 */
public interface ReadableByteBuffer {

  /**
   * Returns the length of this block
   * 
   * @return
   */
  int length();

  /**
   * Returns the byte at position i in the current block
   * 
   * @param i
   * @return
   */
  byte get(int i);

  /**
   * Bulk operation for writing to channel
   * 
   * @param channel
   * @throws IOException
   */
  void write(WritableByteChannel channel) throws IOException;

  /**
   * 
   * @param channel
   * @param offset
   * @param length
   * @throws IOException
   */
  void write(WritableByteChannel channel, int offset, int length) throws IOException;

}
