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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;

import com.salesforce.zsync.internal.util.TransferListener.ResourceTransferListener;

/**
 * An input stream wrapper that lets you observe some things about bytes read from the wrapped
 * {@link InputStream}.
 *
 * @author bstclair
 */
public class ObservableInputStream extends FilterInputStream {

  private final TransferListener observer;

  public ObservableInputStream(InputStream in, TransferListener observer) {
    super(in);
    this.observer = observer;
  }

  @Override
  public int read() throws IOException {
    final int i = super.read();
    if (i >= 0) {
      this.observer.transferred(1);
    }
    return i;
  }

  @Override
  public int read(byte[] b, int off, int len) throws IOException {
    final int i = super.read(b, off, len);
    if (i >= 0) {
      this.observer.transferred(i);
    }
    return i;
  }

  @Override
  public void close() throws IOException {
    try {
      super.close();
    } finally {
      this.observer.close();
    }
  }

  /**
   * An input stream wrapper that lets you observe bytes of a resource with up-front known size.
   *
   * @author bbusjaeger
   */
  public static class ObservableResourceInputStream<T> extends ObservableInputStream {

    public ObservableResourceInputStream(InputStream in, ResourceTransferListener<T> observer, T resource, long size) {
      super(in, observer);
      observer.start(resource, size);
    }
  }

}