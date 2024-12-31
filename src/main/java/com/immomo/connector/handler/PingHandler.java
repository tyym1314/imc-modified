package com.immomo.connector.handler;

import com.immomo.connector.handler.service.SessionListener;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.tunnel.Tunnel;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class PingHandler implements MsgPacketHandler {

  @Autowired
  private SessionListener sessionListener;
  @Resource
  private Tunnel tunnel;

  @Override
  public void handle(MsgBytePacket packet, ClientSession session) throws Exception {
    HubbleUtils.incrCountByClient("ping_msg_req", session.getClient());
    //TODO: limit ping rate
    MsgBytePacket ret = MsgBytePackets.newPong(packet.getHeader().getSeqId());
    session.deliverPong(ret);
    sessionListener.onPing(session);
    HubbleUtils.incrCountByClient("ping_msg_succ", session.getClient());
    tunnel.recordPing(session);
  }
}
