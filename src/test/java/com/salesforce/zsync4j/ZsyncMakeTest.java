package com.salesforce.zsync4j;

import static org.junit.Assert.assertEquals;

import org.junit.Test;


public class ZsyncMakeTest {

  @Test
  public void testWeakChecksumLength() {
    assertEquals(2, ZsyncMake.weakChecksumLength(1024, 2048, 2));
    // tests boundary between 2 and 3 bits at 4096 block size and 2 seq matches (~389MB)
    assertEquals(3, ZsyncMake.weakChecksumLength(389 * 1024 * 1024, 4096, 2));
    // tests boundary between 3 and 4 bits at 4096 block size and 2 seq matches (~25TB)
    assertEquals(4, ZsyncMake.weakChecksumLength(25l * 1024 * 1024 * 1024 * 1024, 4096, 2));
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
