package com.immomo.connector.session;

import io.netty.util.AttributeKey;

public class Const {

  private static final String ATTR_CLIENTSESSION__KEY = "abc-efg-se";

  public static final AttributeKey<ClientSession> CLIENTSESSION =
      AttributeKey.newInstance(Const.ATTR_CLIENTSESSION__KEY);
}
