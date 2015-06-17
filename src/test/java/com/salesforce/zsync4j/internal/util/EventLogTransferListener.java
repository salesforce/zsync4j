package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.salesforce.zsync4j.internal.util.HttpClient.HttpTransferListener;
import com.squareup.okhttp.Request;
import com.squareup.okhttp.Response;

class EventLogTransferListener implements HttpTransferListener {

  static interface Event {
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
  }

  static class Initialized implements EventLogTransferListener.Event {
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

  static class Started implements EventLogTransferListener.Event {
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
      EventLogTransferListener.Started other = (EventLogTransferListener.Started) obj;
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

  static class Progressed implements EventLogTransferListener.Event {
    private final long bytes;

    public Progressed(long bytes) {
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
      Progressed other = (Progressed) obj;
      if (this.bytes != other.bytes) {
        return false;
      }
      return true;
    }

  }

  static class Closed implements EventLogTransferListener.Event {
    static final EventLogTransferListener.Closed INSTANCE = new Closed();

    private Closed() {
      super();
    }
  }

  private final List<EventLogTransferListener.Event> eventLog;

  EventLogTransferListener() {
    this.eventLog = new ArrayList<>();
  }

  public List<EventLogTransferListener.Event> getEventLog() {
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
    this.eventLog.add(new Progressed(bytes));
  }

  @Override
  public void close() {
    this.eventLog.add(Closed.INSTANCE);
  }

}
