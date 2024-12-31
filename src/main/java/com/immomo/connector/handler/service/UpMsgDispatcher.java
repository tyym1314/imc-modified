package com.immomo.connector.handler.service;

/**
 * Created By wlb on 2019-09-25 16:43
 */
public interface UpMsgDispatcher {

  void dispatch(String appId, UpMsg upMsg);

}
