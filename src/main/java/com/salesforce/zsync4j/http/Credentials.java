package com.salesforce.zsync4j.http;

/**
 * Credentials used to authenticate with remote hosts
 *
 * @author bbusjaeger
 */
public class Credentials {
  private final String username;
  private final String password;

  public Credentials(String username, String password) {
    this.username = username;
    this.password = password;
  }

  /**
   * User name for remote authentication
   *
   * @return
   */
  public String getUsername() {
    return this.username;
  }

  /**
   * Password for remote authentication
   *
   * @return
   */
  public String getPassword() {
    return this.password;
  }

  /**
   * Returns basic authorization header
   *
   * @return
   */
  public String basic() {
    return com.squareup.okhttp.Credentials.basic(this.username, this.password);
  }

}
