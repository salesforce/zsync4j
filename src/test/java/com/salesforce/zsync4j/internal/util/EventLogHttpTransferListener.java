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
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.salesforce.zsync4j.internal.util.HttpClient.HttpTransferListener;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

/**
 * A test utility class for recording listener events so that tests can assert the sequence of
 * events received after invoking a method that takes a listener parameter.
 *
 * @author bbusjaeger
 */
class EventLogHttpTransferListener implements HttpTransferListener {

  static interface Event {
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
  }

  static class Initialized implements EventLogHttpTransferListener.Event {
    private final Request request;

    public Initialized(Request request) {
      super();
      this.request = request;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.request == null) ? 0 : this.request.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Initialized other = (Initialized) obj;
      return equals(this.request, other.request);
    }

    private boolean equals(Request r1, Request r2) {
      if (r1 == null && r2 == null) {
        return true;
      }
      if (r1 == null || r2 == null) {
        return false;
      }
      try {
        return r1.urlString().equals(r2.urlString()) && r1.uri().equals(r2.uri()) && r1.url().equals(r2.url())
            && r1.method().equals(r2.method()) && r1.headers().names().equals(r2.headers().names());
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    }
  }

  static class Started implements EventLogHttpTransferListener.Event {
    private final URI uri;
    private final long totalBytes;

    Started(URI uri, long totalBytes) {
      this.uri = uri;
      this.totalBytes = totalBytes;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.totalBytes ^ (this.totalBytes >>> 32));
      result = prime * result + ((this.uri == null) ? 0 : this.uri.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      EventLogHttpTransferListener.Started other = (EventLogHttpTransferListener.Started) obj;
      if (this.totalBytes != other.totalBytes) {
        return false;
      }
      if (this.uri == null) {
        if (other.uri != null) {
          return false;
        }
      } else if (!this.uri.equals(other.uri)) {
        return false;
      }
      return true;
    }

  }

  static class Transferred implements EventLogHttpTransferListener.Event {
    private final long bytes;

    public Transferred(long bytes) {
      super();
      this.bytes = bytes;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + (int) (this.bytes ^ (this.bytes >>> 32));
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null) {
        return false;
      }
      if (getClass() != obj.getClass()) {
        return false;
      }
      Transferred other = (Transferred) obj;
      if (this.bytes != other.bytes) {
        return false;
      }
      return true;
    }

  }

  static class Closed implements EventLogHttpTransferListener.Event {
    static final EventLogHttpTransferListener.Closed INSTANCE = new Closed();

    private Closed() {
      super();
    }
  }

  private final List<EventLogHttpTransferListener.Event> eventLog;

  EventLogHttpTransferListener() {
    this.eventLog = new ArrayList<>();
  }

  public List<EventLogHttpTransferListener.Event> getEventLog() {
    return this.eventLog;
  }

  @Override
  public void initiating(Request request) {
    this.eventLog.add(new Initialized(request));
  }

  @Override
  public void start(Response response, long totalBytes) {
    try {
      this.eventLog.add(new Started(response.request().uri(), totalBytes));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @Override
  public void transferred(long bytes) {
    this.eventLog.add(new Transferred(bytes));
  }

  @Override
  public void close() {
    this.eventLog.add(Closed.INSTANCE);
  }

}
