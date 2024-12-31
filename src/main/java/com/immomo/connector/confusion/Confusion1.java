package com.immomo.connector.confusion;

public class Confusion1 implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {
    int helflen = source.length() / 2;
    return source + source.substring(0, helflen);
  }
}
