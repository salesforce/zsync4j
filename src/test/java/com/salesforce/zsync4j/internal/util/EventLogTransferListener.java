package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;

import com.salesforce.zsync4j.internal.util.TransferListener.ResourceTransferListener;
import com.squareup.okhttp.Response;

class EventLogTransferListener implements ResourceTransferListener<Response> {

  static interface Event {
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
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
