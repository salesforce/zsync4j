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
package com.salesforce.zsync.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.salesforce.zsync.internal.util.TransferListener.ResourceTransferListener;

/**
 * A redable byte channel that lets you observe bytes read from the wrapped
 * {@link ReadableByteChannel}.
 *
 * @author bbusjaeger
 */
public class ObservableRedableByteChannel implements ReadableByteChannel {

  private final ReadableByteChannel channel;
  private final TransferListener listener;

  public ObservableRedableByteChannel(ReadableByteChannel channel, TransferListener listener) {
    this.channel = channel;
    this.listener = listener;
  }

  @Override
  public boolean isOpen() {
    return this.channel.isOpen();
  }

  @Override
  public int read(ByteBuffer dst) throws IOException {
    final int i = this.channel.read(dst);
    if (i >= 0) {
      this.listener.transferred(i);
    }
    return i;
  }

  @Override
  public void close() throws IOException {
    try {
      this.channel.close();
    } finally {
      this.listener.close();
    }
  }

  public static class ObservableReadableResourceChannel<T> extends ObservableRedableByteChannel {

    public ObservableReadableResourceChannel(ReadableByteChannel channel, ResourceTransferListener<T> listener, T resource, long size) {
      super(channel, listener);
      listener.start(resource, size);
    }

  }
}
