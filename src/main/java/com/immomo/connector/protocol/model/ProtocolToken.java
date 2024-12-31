package com.immomo.connector.protocol.model;

public class ProtocolToken {

  public static final byte PING = 1;
  public static final byte PONG = 2;
  public static final byte SAUTH = 3;
  public static final byte SAUTH_RET = 4;
  public static final byte UP_BIZ_MSG = 5;
  public static final byte DOWN_BIZ_MSG = 6;
  public static final byte RET = 7;
  public static final byte KICK = 8;
  public static final byte RECONN = 9;

  //连麦上行消息
  public static final byte LINK_UP = 11;
  //连麦下行消息
  public static final byte LINK_DOWN = 12;

}
