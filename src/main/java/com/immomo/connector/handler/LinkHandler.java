package com.immomo.connector.handler;

import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.immomo.connector.bean.MsgSource;
import com.immomo.connector.constants.Constants;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.tunnel.Tunnel;
import com.immomo.connector.util.MsgIdUtils;
import com.immomo.live.im.connector.bean.PlatformDownProtos;
import com.immomo.live.im.connector.bean.PlatformUpProtos;
import com.immomo.mcf.util.StringUtils;
import com.immomo.moaservice.onelink.api.IOnelinkInnerService;
import com.immomo.moaservice.onelink.api.bean.ProcessorResponse;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created By wlb on 2019-10-17 16:03
 */
@Slf4j
@Component
public class LinkHandler implements MsgPacketHandler {

  @Autowired
  private IOnelinkInnerService onelinkInnerService;

  @Resource
  private Tunnel tunnel;


  @Override
  public void handle(MsgBytePacket packet, ClientSession session) throws Exception {
    long start = System.currentTimeMillis();
    try {
      HubbleUtils.incrCount("linkup_msg_req");
      handle0(packet, session);
      HubbleUtils.incrCount("linkup_msg_succ");
    } finally {
      HubbleUtils.addTime("link-handle-time", System.currentTimeMillis() - start);
    }
  }

  void handle0(MsgBytePacket packet, ClientSession session) throws InvalidProtocolBufferException {
    String linkChannelId = session.getLinkChannelId();
    if (StringUtils.isBlank(linkChannelId)) {
      log.error("linkChannelId is blank, session:{}", session.logInfo());
      session.close(Constants.CloseReason_Normal);
      return;
    }


    PlatformUpProtos.ClientMsg upMsg = PlatformUpProtos.ClientMsg.parseFrom(packet.getBody());
    byte[] linkData = upMsg.getData().toByteArray();

    //先返回一个ret
    MsgBytePacket ret = MsgBytePackets
        .newSuccessRet(packet.getHeader().getSeqId(), MsgIdUtils.id());
    session.deliver(ret, MsgSource.LINKUP);

    //这里有点问题，onelink里面的im走的是live-im-link长连接服务，response只包了group
    ProcessorResponse response = onelinkInnerService
        .linkUpRequest(session.getAppId(), linkChannelId, session.getUserId(), linkData);
    tunnel.recordUp(session, "link", upMsg);
    if (response.isFeedbackable()) {
      byte[] data = response.getData();
      byte[] bytes = PlatformDownProtos.ServerMsg.newBuilder().setMsgid(MsgIdUtils.id())
          .setMsgTime(System.currentTimeMillis()).setData(
              ByteString.copyFrom(data)).build().toByteArray();
      session.deliver(new MsgBytePacket(MsgBytePackets.newDownLinkHeader(), bytes), MsgSource.LINKUP);
    }

  }
}
