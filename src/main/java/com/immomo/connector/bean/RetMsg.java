package com.immomo.connector.bean;

/**
 * @author zhang.bo
 * @since 03 29, 2018
 */
public enum RetMsg {
  // 发言限制（祝艳需求）
  BILI_FREQUENCY_LIMIT(501, "您的发言过于频繁，清稍后再试");

  int ec;
  String em;

  RetMsg(int ec ,String em) {
    this.ec = ec;
    this.em = em;
  }

  public int getEc() {
    return ec;
  }

  public String getEm() {
    return em;
  }

}
