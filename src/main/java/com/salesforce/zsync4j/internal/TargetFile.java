package com.salesforce.zsync4j.internal;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.WRITE;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.channels.Channels;
import java.nio.channels.FileChannel;
import java.nio.channels.ReadableByteChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.ListMultimap;
import com.salesforce.zsync4j.internal.util.ReadableByteBuffer;
import com.salesforce.zsync4j.internal.util.RangeFetcher.RangeReceiver;
import com.salesforce.zsync4j.internal.util.ZsyncUtil;

public class TargetFile implements RangeReceiver, Closeable {

  // immutable state
  private final Path path;
  private final Header header;
  private final List<BlockSum> blockSums;
  private final ListMultimap<BlockSum, Integer> positions;

  // mutable state
  private final FileChannel channel;
  private final boolean[] written;

  public TargetFile(Path path, ControlFile controlFile) throws IOException {
    this.path = path;
    try {
      Files.createDirectories(path.getParent());
    } catch (IOException e) {
      // ignore
    }
    this.channel = FileChannel.open(path, CREATE, WRITE);

    this.header = controlFile.getHeader();
    this.blockSums = ImmutableList.copyOf(controlFile.getBlockSums());
    this.positions = indexPositions(this.blockSums);
    this.written = new boolean[blockSums.size()];
  }

  static ListMultimap<BlockSum, Integer> indexPositions(List<BlockSum> blockSums) {
    final ImmutableListMultimap.Builder<BlockSum, Integer> b = ImmutableListMultimap.builder();
    for (int i = 0; i < blockSums.size(); i++)
      b.put(blockSums.get(i), i);
    return b.build();
  }

  public int getNumBlocks() {
    return blockSums.size();
  }

  public BlockSum get(int index) {
    return blockSums.get(index);
  }

  public List<Integer> getPositions(BlockSum sum) {
    return positions.get(sum);
  }

  public boolean write(int position, ReadableByteBuffer data) {
    return write(position, data, 0, data.length());
  }

  public boolean write(int position, ReadableByteBuffer data, int offset, int length) {
    if (written[position])
      return false;
    try {
      channel.position(position * header.getBlocksize());
      data.write(channel, offset, length);
    } catch (IOException e) {
      throw new RuntimeException("Failed to read block at position " + position, e);
    }
    return written[position] = true;
  }

  public List<Range> getMissingRanges() {
    final int blockSize = header.getBlocksize();
    final ImmutableList.Builder<Range> b = ImmutableList.builder();
    long start = -1;
    for (int i = 0; i < written.length; i++) {
      if (written[i]) {
        // if we're in a range, end it
        if (start != -1) {
          b.add(new Range(start, i * blockSize - 1));
          start = -1;
        }
      } else {
        // if we're not in a range, start one
        if (start == -1) {
          start = i * blockSize;
        }
        // if this is the last block in the file map, we need to end the range
        if (i == written.length - 1) {
          b.add(new Range(start, header.getLength() - 1));
        }
      }
    }
    return b.build();
  }

  @Override
  public void receive(Range range, InputStream in) throws IOException {
    if (range.first % header.getBlocksize() != 0)
      throw new RuntimeException("Invalid range received: first byte not block aligned");
    if ((range.last + 1) % header.getBlocksize() != 0 && range.last + 1 != header.getLength())
      throw new RuntimeException("Invalid range received: last byte not block aligned");

    // TODO use single buffer
    final ByteBuffer b = ByteBuffer.allocate((int) range.size());
    ReadableByteChannel c = Channels.newChannel(in);
    while (b.hasRemaining())
      if (c.read(b) == -1)
        break;
    b.flip();
    channel.position(range.first);
    while (b.hasRemaining())
      channel.write(b);

    final int first = (int) (range.first / header.getBlocksize());
    final int last = (int) (range.last + 1 == header.getLength() ? written.length - 1 : (range.last + 1) / header.getBlocksize() - 1);
    for (int i = first; i <= last; i++) {
      written[i] = true;
    }
  }

  @Override
  public void close() throws IOException {
    final List<Range> missingRanges = getMissingRanges();
    if (!missingRanges.isEmpty())
      throw new IllegalStateException("Missing ranges in target file: " + missingRanges);
    if (!header.getSha1().equals(ZsyncUtil.computeSha1(path)))
      throw new IllegalStateException("Target file sha1 does not match expected");
  }
}
