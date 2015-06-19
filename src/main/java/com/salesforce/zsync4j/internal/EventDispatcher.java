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
import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.salesforce.zsync4j.Zsync.Options;
import com.salesforce.zsync4j.ZsyncObserver;
import com.salesforce.zsync4j.http.ContentRange;
import com.salesforce.zsync4j.internal.util.HttpClient.HttpTransferListener;
import com.salesforce.zsync4j.internal.util.HttpClient.RangeReceiver;
import com.salesforce.zsync4j.internal.util.HttpClient.RangeTransferListener;
import com.salesforce.zsync4j.internal.util.TransferListener.ResourceTransferListener;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;


/**
 * A {@link ZsyncObserver} that forwards observed events to a configurable list of additional zsync
 * observers.
 *
 * @author bstclair
 */
public class EventDispatcher {

  private final ZsyncObserver observer;

  public EventDispatcher(ZsyncObserver observer) {
    this.observer = observer;
  }

  public void zsyncStarted(URI requestedZsyncUri, Options options) {
    this.observer.zsyncStarted(requestedZsyncUri, options);
  }

  public void zsyncFailed(Exception exception) {
    this.observer.zsyncFailed(exception);
  }

  public void zsyncComplete() {
    this.observer.zsyncComplete();
  }

  public ResourceTransferListener<Path> getControlFileReadListener() {
    return new ResourceTransferListener<Path>() {
      @Override
      public void start(Path resource, long length) {
        EventDispatcher.this.observer.controlFileReadingStarted(resource, length);
      }

      @Override
      public void transferred(long bytes) {
        EventDispatcher.this.observer.bytesRead(bytes);
      }

      @Override
      public void close() throws IOException {
        EventDispatcher.this.observer.controlFileReadingComplete();
      }
    };
  }

  public HttpTransferListener getControlFileDownloadListener() {
    return new HttpTransferListener() {

      @Override
      public void initiating(Request request) {
        try {
          EventDispatcher.this.observer.controlFileDownloadingInitiated(request.uri());
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void start(Response response, long length) {
        try {
          EventDispatcher.this.observer.controlFileDownloadingStarted(response.request().uri(), length);
        } catch (IOException e) {
          throw new RuntimeException(e);
        }
      }

      @Override
      public void transferred(long bytes) {
        EventDispatcher.this.observer.bytesDownloaded(bytes);
      }

      @Override
      public void close() throws IOException {
        EventDispatcher.this.observer.controlFileDownloadingComplete();
      }
    };
  }

  public ResourceTransferListener<Path> getOutputFileWriteListener() {
    return new ResourceTransferListener<Path>() {

      @Override
      public void start(Path path, long length) {
        EventDispatcher.this.observer.outputFileWritingStarted(path, length);
      }

      @Override
      public void transferred(long bytes) {
        EventDispatcher.this.observer.bytesWritten(bytes);
      }

      @Override
      public void close() throws IOException {
        EventDispatcher.this.observer.outputFileWritingCompleted();
      }
    };
  }

  public ResourceTransferListener<Path> getInputFileReadListener() {
    return new ResourceTransferListener<Path>() {
      @Override
      public void start(Path resource, long length) {
        EventDispatcher.this.observer.inputFileReadingStarted(resource, length);
      }

      @Override
      public void transferred(long bytes) {
        EventDispatcher.this.observer.bytesRead(bytes);
      }

      @Override
      public void close() throws IOException {
        EventDispatcher.this.observer.inputFileReadingComplete();
      }
    };
  }

  public RangeTransferListener getRemoteFileDownloadListener() {
    return new RangeTransferListener() {
      @Override
      public HttpTransferListener newTransfer(final List<ContentRange> ranges) {
        return new HttpTransferListener() {
          @Override
          public void initiating(Request request) {
            try {
              EventDispatcher.this.observer.remoteFileDownloadingInitiated(request.uri(), ranges);
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }

          @Override
          public void start(Response resource, long length) {
            try {
              EventDispatcher.this.observer.remoteFileDownloadingStarted(resource.request().uri(), resource.body()
                  .contentLength());
            } catch (IOException e) {
              throw new RuntimeException(e);
            }
          }

          @Override
          public void transferred(long bytes) {
            EventDispatcher.this.observer.bytesDownloaded(bytes);
          }

          @Override
          public void close() throws IOException {
            EventDispatcher.this.observer.remoteFileDownloadingComplete();
          }
        };
      }
    };
  }

  public RangeReceiver getRangeReceiverListener(final RangeReceiver rangeReceiver) {
    return new RangeReceiver() {
      @Override
      public void receive(ContentRange range, InputStream in) throws IOException {
        rangeReceiver.receive(range, in);
        EventDispatcher.this.observer.remoteFileRangeReceived(range);
      }
    };
  }
}
