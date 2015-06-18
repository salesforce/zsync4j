package com.salesforce.zsync4j.http;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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
  public void testSize() {
    assertEquals(2, new ContentRange(1, 2).size());
  }

  @Test
  public void testSizeSingle() {
    assertEquals(1, new ContentRange(2, 2).size());
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
