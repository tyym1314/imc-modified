package com.immomo.connector.handler.service.impl;

import com.immomo.connector.handler.component.KafkaProducer;
import com.immomo.connector.handler.service.SessionListener;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.util.ImIpUtils;
import com.immomo.connector.util.Unit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created By wlb on 2019-11-15 14:45
 */
@Slf4j
@Service
public class SessionListenerImpl implements SessionListener {

  @Autowired
  private KafkaProducer kafkaProducer;

  @Override
  public void onRegister(ClientSession session) {
    enter(session);
  }

  @Override
  public void onUnregister(ClientSession session) {
    exit(session);
  }

  @Override
  public void onPing(ClientSession session) {
    ping(session);
  }

  /**
   * 用户进入房间
   */
  private Unit enter(ClientSession session) {
    Unit unit = new Unit("enter");
    wrapBase(unit, session);
    sendToKafka(session, unit);
    return unit;
  }

  private void sendToKafka(ClientSession session, Unit unit) {
    kafkaProducer.sendAsync("event_im_platform_connector", session.getUserId(), unit.toJson());
  }


  /**
   * 用户退出房间
   */
  private Unit exit(ClientSession session) {
    Unit unit = new Unit("exit");
    wrapBase(unit, session);
    unit.setParam("close_reason", session.getCloseReason());
    unit.setParam("newRoomId", session.getNewRoomId());
    sendToKafka(session, unit);
    return unit;
  }

  /**
   * ping
   */
  public void ping(ClientSession session) {
    Unit unit = new Unit("ping");
    wrapBase(unit, session);
    unit.setParam("lastPingTs", session.getLastPingTs().get());
    sendToKafka(session, unit);
  }

  private void wrapBase(Unit unit, ClientSession session) {
    unit.setParam("appId", session.getAppId());
    unit.setParam("userId", session.getUserId());
    unit.setParam("roomId", session.getRoomId());
    unit.setParam("channelId", session.getLinkChannelId());
    unit.setParam("ip", session.getAddress());
    unit.setParam("client", session.getClient().name());
    unit.setParam("v", session.getClientVersion());
    unit.setParam("host", ImIpUtils.IP_LAN);
    unit.setParam("sauthTs", session.getSauthTime());
  }
}
