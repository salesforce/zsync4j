package com.salesforce.zsync4j.internal.util;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class RangeFetcherTest {

  @Test
  public void exceptionThrownFromConstructorForNullHttpClient() {
    // Act
    try {
      new RangeFetcher(null);
    } catch (IllegalArgumentException exception) {
      
      // Assert
      assertEquals("httpClient cannot be null", exception.getMessage());
    }
  }
}
