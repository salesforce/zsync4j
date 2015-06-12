package com.salesforce.zsync4j.internal.util;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

import com.salesforce.zsync4j.internal.util.TransferListener.ResourceTransferListener;

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
