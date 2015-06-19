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
