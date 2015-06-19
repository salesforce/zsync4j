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
