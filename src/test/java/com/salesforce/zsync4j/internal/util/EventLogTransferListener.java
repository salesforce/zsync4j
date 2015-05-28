package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.salesforce.zsync4j.internal.util.HttpClient.ResponseBodyTransferListener;

class EventLogTransferListener implements ResponseBodyTransferListener {

  static interface Event {
    @Override
    boolean equals(Object obj);

    @Override
    int hashCode();
  }

  static class Started implements EventLogTransferListener.Event {
    private final String uri;
    private final long totalBytes;

    Started(String uri, long totalBytes) {
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
    private final int bytes;

    public Progressed(int bytes) {
      super();
      this.bytes = bytes;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + this.bytes;
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
      EventLogTransferListener.Progressed other = (EventLogTransferListener.Progressed) obj;
      if (this.bytes != other.bytes) {
        return false;
      }
      return true;
    }

  }

  static class Completed implements EventLogTransferListener.Event {
    static final EventLogTransferListener.Completed INSTANCE = new Completed();

    private Completed() {
      super();
    }
  }

  static class Failed implements EventLogTransferListener.Event {
    private final IOException exception;

    public Failed(IOException exception) {
      super();
      this.exception = exception;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((this.exception == null) ? 0 : this.exception.hashCode());
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
      EventLogTransferListener.Failed other = (EventLogTransferListener.Failed) obj;
      if (this.exception == null) {
        if (other.exception != null) {
          return false;
        }
      } else if (!this.exception.equals(other.exception)) {
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
  public void transferStarted(String uri, long totalBytes) {
    this.eventLog.add(new Started(uri, totalBytes));
  }

  @Override
  public void transferProgressed(int bytes) {
    this.eventLog.add(new Progressed(bytes));
  }

  @Override
  public void transferFailed(IOException exception) {
    this.eventLog.add(new Failed(exception));
  }

  @Override
  public void transferComplete() {
    this.eventLog.add(Completed.INSTANCE);
  }

  @Override
  public void transferClosed() {
    this.eventLog.add(Closed.INSTANCE);
  }

}