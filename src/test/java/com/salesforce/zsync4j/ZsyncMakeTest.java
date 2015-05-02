package com.salesforce.zsync4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ZsyncMakeTest {

  @Test
  public void testWeakChecksumLength() {
    assertEquals(2, ZsyncMake.weakChecksumLength(1024, 2048, 2));
    assertEquals(3, ZsyncMake.weakChecksumLength(500 * 1024 * 1024, 4096, 2));
    assertEquals(4, ZsyncMake.weakChecksumLength(100 * 1024 * 1024, 4096, 1));
  }

  @Test
  public void testStrongChecksumLength() {
    assertEquals(3, ZsyncMake.strongChecksumLength(1024, 2048, 2));
    assertEquals(5, ZsyncMake.strongChecksumLength(100 * 1024 * 1024, 4096, 2));
    assertEquals(8, ZsyncMake.strongChecksumLength(100 * 1024 * 1024, 4096, 1));
    assertEquals(16, ZsyncMake.strongChecksumLength(Long.MAX_VALUE, 4096, 1));

    assertEquals(5, ZsyncMake.strongChecksumLength(57323443l, 2048, 2));
  }
  
}
