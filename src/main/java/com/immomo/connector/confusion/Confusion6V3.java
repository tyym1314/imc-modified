package com.immomo.connector.confusion;

public class Confusion6V3 implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {
    return source.substring(0, 24);
  }
}
