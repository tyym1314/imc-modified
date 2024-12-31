package com.immomo.connector.handler.service;

import com.immomo.connector.session.ClientSession;

/**
 * Created By wlb on 2019-11-15 14:42
 */
public interface SessionListener {

  void onRegister(ClientSession session);

  void onUnregister(ClientSession session);

  void onPing(ClientSession session);
}
