package com.immomo.connector.handler.service;

/**
 * Created By wlb on 2019-09-19 12:12
 */
public interface SauthService {

  boolean auth(String appId, String userId, String userToken);

}