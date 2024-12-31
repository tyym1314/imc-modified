package com.immomo.connector.handler;

import com.immomo.connector.protocol.model.ProtocolToken;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import com.immomo.env.MomoEnv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created By wlb on 2019-10-22 15:55
 */
@Slf4j
@Component
public class MessageDispatcher implements MsgPacketHandler {

  @Autowired
  private SauthHandler sauthHandler;
  @Autowired
  private PingHandler pingHandler;
  @Autowired
  private UpMsgHandler upMsgHandler;
  @Autowired
  private LinkHandler linkHandler;

  @Override
  public void handle(MsgBytePacket packet, ClientSession session) throws Exception {
    /**
     * TODO
     * 1. 根据appId分配线程池资源
     */

    byte msgType = packet.getHeader().getMsgType();
    if (ProtocolToken.PING == msgType) {
      pingHandler.handle(packet, session);
    } else if (ProtocolToken.SAUTH == msgType) {
      sauthHandler.handle(packet, session);
    } else if (ProtocolToken.UP_BIZ_MSG == msgType) {
      upMsgHandler.handle(packet, session);
    } else if (ProtocolToken.LINK_UP == msgType) {
      if ("alpha".equals(MomoEnv.corp())) {
        log.info("linkUp:{}", session.toString());
      }
      linkHandler.handle(packet, session);
    } else {
      log.warn("unknown msgTpye:{}, session:{}", msgType, session);
    }
  }
}
