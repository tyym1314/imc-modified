package com.immomo.connector.handler.service;

import java.io.Serializable;

/**
 * Created By wlb on 2019-09-19 12:13
 */
public class SauthParam implements Serializable {

  private static final long serialVersionUID = -1L;

  private String userId;
  private String token;
  //TODO:其他参数

  public SauthParam() {
  }

  public SauthParam(String userId, String token) {
    this.userId = userId;
    this.token = token;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getToken() {
    return token;
  }

  public void setToken(String token) {
    this.token = token;
  }
}
