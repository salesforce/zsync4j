/**
 * Copyright © 2015 salesforce.com, inc. All rights reserved.
 */
package com.salesforce.zsync4j.internal;

import java.net.URI;
import java.nio.file.Path;
import java.util.List;

import com.salesforce.zsync4j.Zsync.Options;


/**
 * Describe your class here.
 *
 * @author bstclair
 */
public class EventManagerImpl implements EventManager {

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#transferStarted(java.net.URI,
   * com.salesforce.zsync4j.Zsync.Options)
   */
  @Override
  public void transferStarted(URI requestedZsyncUri, Options options) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#controlFileDownloadStarted(java.net.URI)
   */
  @Override
  public void controlFileDownloadStarted(URI controlFileUri) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.salesforce.zsync4j.internal.EventManager#controlFileDownloadComplete(com.salesforce.zsync4j
   * .internal.ControlFile)
   */
  @Override
  public void controlFileDownloadComplete(ControlFile controlFile) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.salesforce.zsync4j.internal.EventManager#inputFileProcessingStarted(java.nio.file.Path)
   */
  @Override
  public void inputFileProcessingStarted(Path inputFile) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.salesforce.zsync4j.internal.EventManager#inputFileProcessingComplete(java.nio.file.Path)
   */
  @Override
  public void inputFileProcessingComplete(Path inputFile) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#remoteFileProcessingStarted(java.net.URI,
   * long, long, long)
   */
  @Override
  public void remoteFileProcessingStarted(URI remoteUri, long expectedBytes, long expectedBlocks, long expectedRequests) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#remoteFileProcessingComplete()
   */
  @Override
  public void remoteFileProcessingComplete() {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#blocksRequestStarted(java.util.List)
   */
  @Override
  public void blocksRequestStarted(List<Range> blocks) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#blocksRequestComplete(java.util.List)
   */
  @Override
  public void blocksRequestComplete(List<Range> blocks) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.salesforce.zsync4j.internal.EventManager#blockProcessingStarted(com.salesforce.zsync4j.
   * internal.Range)
   */
  @Override
  public void blockProcessingStarted(Range block) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see
   * com.salesforce.zsync4j.internal.EventManager#blockProcessingComplete(com.salesforce.zsync4j
   * .internal.Range)
   */
  @Override
  public void blockProcessingComplete(Range block) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#bytesDownloaded(long)
   */
  @Override
  public void bytesDownloaded(long bytes) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#bytesWritten(java.nio.file.Path, long)
   */
  @Override
  public void bytesWritten(Path file, long bytes) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#sha1CalculationStarted(java.nio.file.Path)
   */
  @Override
  public void sha1CalculationStarted(Path file) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#sha1CalculationComplete(java.lang.String)
   */
  @Override
  public void sha1CalculationComplete(String sha1) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#moveTempFileStarted(java.nio.file.Path,
   * java.nio.file.Path)
   */
  @Override
  public void moveTempFileStarted(Path tempFile, Path targetFile) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#moveTempFileComplete()
   */
  @Override
  public void moveTempFileComplete() {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#transferFailed(java.lang.Exception)
   */
  @Override
  public void transferFailed(Exception exception) {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see com.salesforce.zsync4j.internal.EventManager#transferComplete()
   */
  @Override
  public void transferComplete() {
    // TODO Auto-generated method stub

  }
}
