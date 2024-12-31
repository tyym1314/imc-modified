package com.immomo.connector.confusion;

public class Confusion8V3 implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {
    return source.substring(20) + source.substring(0, 12);
  }
}
