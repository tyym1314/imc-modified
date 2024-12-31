package com.immomo.connector.session;

import com.immomo.mcf.util.StringUtils;

public enum Client {

  ANDROID, IOS, OTHER;

  public static Client forClient(String client) {
    if (StringUtils.isBlank(client)) {
      return OTHER;
    }

    String clientUpper = client.toUpperCase();
    if ("ANDROID".equals(clientUpper)) {
      return ANDROID;
    } else if ("IOS".equals(clientUpper)) {
      return IOS;
    }

    return OTHER;
  }
}
