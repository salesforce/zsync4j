package com.salesforce.zsync4j.http;

import static org.junit.Assert.assertEquals;

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

}
