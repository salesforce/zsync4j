/**
 * Copyright (c) 2015, Salesforce.cimport static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
lowing conditions are met:
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
package com.salesforce.zsync.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import com.salesforce.zsync.http.ContentRange;

public class ContentRangeTest {

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorFirstAfterLast() {
    new ContentRange(2, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorFirstNegative() {
    new ContentRange(-1, 1);
  }

  @Test(expected = IllegalArgumentException.class)
  public void testConstructorSecondNegative() {
    new ContentRange(1, -1);
  }

  @Test
  public void testLength() {
    assertEquals(2, new ContentRange(1, 2).length());
  }

  @Test
  public void testLengthSingle() {
    assertEquals(1, new ContentRange(2, 2).length());
  }

  @Test
  public void testSame() {
    final ContentRange r = new ContentRange(1, 2);
    assertTrue(r.equals(r));
  }

  @Test
  public void testUnequalNull() {
    assertFalse(new ContentRange(1, 2).equals(null));
  }

  @Test
  public void testUnequalOtherType() {
    assertFalse(new ContentRange(1, 2).equals(1));
  }

  @Test
  public void testUnequalFirst() {
    assertFalse(new ContentRange(1, 3).equals(new ContentRange(2, 3)));
  }

  @Test
  public void testUnequalSecond() {
    assertFalse(new ContentRange(1, 2).equals(new ContentRange(1, 3)));
  }

  @Test
  public void testEqual() {
    assertTrue(new ContentRange(1, 2).equals(new ContentRange(1, 2)));
  }

}
