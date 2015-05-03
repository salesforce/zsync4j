package com.salesforce.zsync4j.md4;

import java.security.Provider;

import org.apache.mina.proxy.utils.MD4;

public class MD4Provider extends Provider {

  private static final long serialVersionUID = 881797308415520563L;

  public MD4Provider() {
    super("MD4Provider", 1d, "implements md4");
    put("MessageDigest.MD4", MD4.class.getName());
  }

}
