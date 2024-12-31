package com.immomo.connector.handler;

import com.google.protobuf.InvalidProtocolBufferException;
import com.immomo.connector.bean.MsgSource;
import com.immomo.connector.handler.service.UpMsg;
import com.immomo.connector.handler.service.UpMsgDispatcher;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.tunnel.Tunnel;
import com.immomo.connector.util.MsgIdUtils;
import com.immomo.live.im.connector.bean.PlatformUpProtos.ClientMsg;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * 上行业务消息处理
 *
 * Created By wlb on 2019-09-25 15:05
 */
@Slf4j
@Component
public class UpMsgHandler implements MsgPacketHandler {

  @Autowired
  private UpMsgDispatcher upMsgDispatcher;

  @Resource
  private Tunnel tunnel;

  @Override
  public void handle(MsgBytePacket packet, ClientSession session) throws Exception {
    long start = System.currentTimeMillis();
    try {
      handle0(packet, session);
    } finally {
      HubbleUtils.addTime("upmsg-handle-time", System.currentTimeMillis() - start);
    }
  }

  void handle0(MsgBytePacket packet, ClientSession session) throws InvalidProtocolBufferException {
    ClientMsg clientMsg = ClientMsg.parseFrom(packet.getBody());
    HubbleUtils.incrCountByClient("upmsg_req", session.getClient());
    upMsgDispatcher.dispatch(session.getAppId(),
        new UpMsg(session.getUserId(), session.getRoomId(), session.getLinkChannelId(),
            clientMsg.getData().toByteArray()));

    MsgBytePacket ret = MsgBytePackets
        .newSuccessRet(packet.getHeader().getSeqId(), MsgIdUtils.id());
    session.deliver(ret, MsgSource.CUSTOM_UP);
    HubbleUtils.incrCountByClient("upmsg_succ", session.getClient());
    tunnel.recordUp(session, "UpMsg", clientMsg);
  }


}
