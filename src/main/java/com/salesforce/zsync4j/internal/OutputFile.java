package com.salesforce.zsync4j.internal;

import static com.salesforce.zsync4j.OutputFileListener.OutputFileEvent.bytesDownloadedEvent;
import static com.salesforce.zsync4j.OutputFileListener.OutputFileEvent.bytesWrittenEvent;
import static com.salesforce.zsync4j.internal.util.ZsyncUtil.mkdirs;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.READ;
import static java.nio.file.StandardOpenOption.WRITE;
import static java.nio.file.attribute.FileTime.fromMillis;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.salesforce.zsync4j.OutputFileListener;
import com.salesforce.zsync4j.OutputFileValidationException;
import com.salesforce.zsync4j.internal.util.RangeFetcher.RangeReceiver;
import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;
import com.salesforce.zsync4j.internal.util.ZsyncUtil;

public class OutputFile implements RangeReceiver, Closeable {

  // immutable state
  private final Path path;
  private final URI remoteUri;
  private final Path tempPath;

  private final int blockSize;
  private final int lastBlockSize;
  private final long length;
  private final String sha1;
  private final long mtime;
  private final List<BlockSum> blockSums;
  private final ListMultimap<BlockSum, Integer> positions;
  private final OutputFileListener outputFileListener;
  // mutable state
  private final FileChannel channel;
  private final boolean[] completed;
  private int blocksRemaining;

  public OutputFile(Path path, ControlFile controlFile, URI remoteUri, OutputFileListener outputFileListener)
      throws IOException {
    this.path = path;
    this.remoteUri = remoteUri;
    this.outputFileListener = outputFileListener;
    this.tempPath = path.getParent().resolve(path.getFileName().toString() + ".part");
    mkdirs(path.getParent());
    this.channel = FileChannel.open(this.tempPath, CREATE, WRITE, READ);

    final Header header = controlFile.getHeader();
    this.blockSize = header.getBlocksize();
    this.length = header.getLength();
    this.lastBlockSize = (int) (this.length % this.blockSize == 0 ? this.blockSize : this.length % this.blockSize);
    this.sha1 = header.getSha1();
    this.mtime = header.getMtime().getTime();

    this.blockSums = ImmutableList.copyOf(controlFile.getBlockSums());
    this.positions = indexPositions(this.blockSums);
    this.completed = new boolean[this.blockSums.size()];
    this.blocksRemaining = this.completed.length;
  }

  static ListMultimap<BlockSum, Integer> indexPositions(List<BlockSum> blockSums) {
    final ImmutableListMultimap.Builder<BlockSum, Integer> b = ImmutableListMultimap.builder();
    for (int i = 0; i < blockSums.size(); i++) {
      b.put(blockSums.get(i), i);
    }
    return b.build();
  }

  public int getNumBlocks() {
    return this.blockSums.size();
  }

  public BlockSum get(int index) {
    return this.blockSums.get(index);
  }

  public List<Integer> getPositions(BlockSum sum) {
    return this.positions.get(sum);
  }

  public boolean writeBlock(int position, ReadableByteBuffer data) {
    return this.writeBlock(position, data, 0);
  }

  public boolean writeBlock(int position, ReadableByteBuffer data, int offset) {
    if (this.completed[position]) {
      return false;
    }
    final int l = position == this.completed.length - 1 ? this.lastBlockSize : this.blockSize;
    try {
      this.channel.position(position * this.blockSize);
      data.write(this.channel, offset, l);
      this.outputFileListener.bytesWritten(bytesWrittenEvent(this.path, this.remoteUri, this.length, this.blockSize));
    } catch (IOException e) {
      throw new RuntimeException("Failed to read block at position " + position, e);
    }
    this.blocksRemaining--;
    return this.completed[position] = true;
  }

  public List<Range> getMissingRanges() {
    final ImmutableList.Builder<Range> b = ImmutableList.builder();
    long start = -1;
    for (int i = 0; i < this.completed.length; i++) {
      if (this.completed[i]) {
        // if we're in a range, end it
        if (start != -1) {
          b.add(new Range(start, i * this.blockSize - 1));
          start = -1;
        }
      } else {
        // if we're not in a range, start one
        if (start == -1) {
          start = i * this.blockSize;
        }
        // if this is the last block in the file map, we need to end the range
        if (i == this.completed.length - 1) {
          b.add(new Range(start, this.length - 1));
        }
      }
    }
    return b.build();
  }

  public boolean isComplete() {
    return this.blocksRemaining == 0;
  }

  @Override
  public void receive(Range range, InputStream in) throws IOException {
    if (range.first % this.blockSize != 0) {
      throw new RuntimeException("Invalid range received: first byte not block aligned");
    }
    if ((range.last + 1) % this.blockSize != 0 && range.last + 1 != this.length) {
      throw new RuntimeException("Invalid range received: last byte not block aligned");
    }

    final ReadableByteChannel src = Channels.newChannel(in);
    final long size = range.size();
    long remaining = size;
    do {
      long transferred = this.channel.transferFrom(src, range.first, size);
      remaining -= transferred;
      this.outputFileListener
          .bytesDownloaded(bytesDownloadedEvent(this.path, this.remoteUri, this.length, transferred));
      this.outputFileListener.bytesWritten(bytesWrittenEvent(this.path, this.remoteUri, this.length, transferred));
    } while (remaining > 0);

    final int first = (int) (range.first / this.blockSize);
    final int last =
        (int) (range.last + 1 == this.length ? this.completed.length - 1 : (range.last + 1) / this.blockSize - 1);
    for (int i = first; i <= last; i++) {
      if (!this.completed[i]) {
        this.blocksRemaining--;
        this.completed[i] = true;
      }
    }
  }

  @Override
  public void close() throws IOException {
    try {
      if (!this.isComplete()) {
        throw new OutputFileValidationException("Target incomplete: missing ranges: " + this.getMissingRanges());
      }
      this.channel.position(0); // reset channel to beginning to compute full SHA1
      if (!this.sha1.equals(ZsyncUtil.computeSha1(this.channel))) {
        throw new OutputFileValidationException("Target file sha1 does not match expected");
      }
    } finally {
      this.channel.close();
    }
    Files.move(this.tempPath, this.path, REPLACE_EXISTING, REPLACE_EXISTING);
    Files.setLastModifiedTime(this.path, fromMillis(this.mtime));
  }
}
