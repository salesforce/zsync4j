package com.salesforce.zsync4j.http;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class CredentialsTest {

  @Test(expected = NullPointerException.class)
  public void testNullUsername() {
    new Credentials(null, "");
  }

  @Test(expected = NullPointerException.class)
  public void testNullPassword() {
    new Credentials(null, "");
  }

  @Test
  public void testBasic() {
    assertEquals(com.squareup.okhttp.Credentials.basic("jdoe", "secret"), new Credentials("jdoe", "secret").basic());
  }

}
