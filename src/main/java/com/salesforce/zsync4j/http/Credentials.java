package com.salesforce.zsync4j.http;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Credentials used to authenticate with remote http hosts
 *
 * @author bbusjaeger
 */
public class Credentials {

  private final String username;
  private final String password;

  public Credentials(String username, String password) {
    checkNotNull(username);
    checkNotNull(password);
    this.username = username;
    this.password = password;
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