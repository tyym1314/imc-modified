package com.immomo.connector.confusion;

public class Confusion7V3 implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {
    return source.substring(8);
  }
}
