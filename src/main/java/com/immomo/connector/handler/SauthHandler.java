package com.immomo.connector.handler;

import com.google.common.base.Preconditions;
import com.google.protobuf.InvalidProtocolBufferException;
import com.googlecode.protobuf.format.JsonFormat;
import com.immomo.connector.constants.Constants;
import com.immomo.connector.dao.IUserDao;
import com.immomo.connector.dao.RoomDao;
import com.immomo.connector.handler.component.IConfig;
import com.immomo.connector.handler.service.SauthService;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.Client;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.session.SessionManager;
import com.immomo.connector.tunnel.Tunnel;
import com.immomo.live.im.connector.bean.PlatformDownProtos;
import com.immomo.live.im.connector.bean.PlatformUpProtos;
import com.immomo.live.im.connector.bean.PlatformUpProtos.Sauth;
import com.immomo.live.im.connector.bean.PlatformUpProtos.Sauth.LinkAuth;
import com.immomo.moaservice.onelink.api.IOnelinkInnerService;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

@Service
@Slf4j
public class SauthHandler implements MsgPacketHandler {

  @Autowired
  private SauthService sauthService;
  @Autowired
  private SessionManager sessionManager;
  @Autowired
  private IOnelinkInnerService onelinkInnerService;
  @Autowired
  private IConfig config;
  @Resource
  private Tunnel tunnel;

  @Autowired
  private IUserDao userDao;

  @Resource
  private RoomDao roomDao;

  @Override
  public void handle(MsgBytePacket packet, ClientSession clientSession) throws Exception {
    long start = System.currentTimeMillis();
    try {
      handle0(packet, clientSession);
    } finally {
      long costTime = System.currentTimeMillis() - start;
      HubbleUtils.addTime("sauth-handle-time", costTime);
    }
  }

  private void handle0(MsgBytePacket packet, ClientSession session)
      throws InvalidProtocolBufferException {
    HubbleUtils.incrCountByClient("auth_msg_req", session.getClient());
    if (!session.getChannel().isActive() || session.isAuthed()) {
      return;
    }
    HubbleUtils.incrCountByClient("auth_msg_req2", session.getClient());

    PlatformUpProtos.Sauth sauth = PlatformUpProtos.Sauth.parseFrom(packet.getBody());
    String sauthStr = JsonFormat.printToString(sauth);
    log.info("sauth request:{},client ip:{}", sauthStr,
        session.getAddress());
    //check param
    checkParam(sauth);
    long msgTime = sauth.getMsgTime();
    long currentTimeMillis = System.currentTimeMillis();
    long costTime = currentTimeMillis - msgTime;
    //防止客户端改时间
    if (costTime >= 200 && costTime < TimeUnit.MINUTES.toMillis(1)) {
      //log.error("longSauthTimeCost:{}, {}", session.toString(), sauthStr);
      HubbleUtils.incrCount("longSauthTimeCost");
    }
    HubbleUtils.incrCountByClient("auth_msg_req3", session.getClient());

    String appId = sauth.getAppId();
    String userId = sauth.getUserId();

    boolean auth = sauthService.auth(appId, userId, sauth.getToken());
    if (!auth) {
      log.error("sauth failed, token illegal, client ip:{}, userId:{}, appId:{}",
          session.getAddress(), userId, appId);
      session.close(Constants.CloseReason_Normal);
      return;
    }
    HubbleUtils.incrCountByClient("auth_msg_pass", session.getClient());

//    LinkAuth linkAuth = sauth.getLinkAuth();
//    if (sauth.hasLinkAuth() && linkAuth != null) {
//      boolean linkAuthSuccess = onelinkInnerService
//          .authSession(linkAuth.getLinkBusinessId(), linkAuth.getLinkSession(),
//              linkAuth.getLinkChannelId(), userId);
//      if (!linkAuthSuccess) {
//        log.error("link sauth failed, link:{}", JsonFormat.printToString(linkAuth));
//        session.close(Constants.CloseReason_Normal);
//        return;
//      }
//    }

    doSauth(session, sauth);

    deliverResponse(session, packet, sauth);
    HubbleUtils.incrCountByClient("auth_msg_succ", session.getClient());

    log.info("sauthsuccess, userId:{}, appId:{}", userId, appId);
  }

  private void checkParam(Sauth sauth) {
    Preconditions.checkArgument(StringUtils.isNotBlank(sauth.getAppId()), "appId empty");
    Preconditions.checkArgument(StringUtils.isNotBlank(sauth.getToken()), "token empty");
    Preconditions.checkArgument(StringUtils.isNotBlank(sauth.getRoomId()), "roomId empty");
    Preconditions.checkArgument(StringUtils.isNotBlank(sauth.getUserId()), "userId empty");
  }

  private void doSauth(ClientSession session, PlatformUpProtos.Sauth sauth) {
    Client client = Client.forClient(sauth.getClient());
    session.setClient(client);
    session.setClientVersion(sauth.getClientVersion());
    session.setAppId(sauth.getAppId());
    session.setUserId(sauth.getUserId());
    session.setRoomId(sauth.getRoomId());
    session.setDeviceId(sauth.getDeviceId());
    session.setUa(sauth.getUa());
    session.setSrc(sauth.getSrc());
    session.setNetType(sauth.getNetType());

    if (sauth.getLinkAuth() != null) {
      session.setLinkChannelId(sauth.getLinkAuth().getLinkChannelId());
    }

    int verifyVersionInt = Integer.MAX_VALUE;
    try {
      String verifyVersion = config.getConfig("iOS_Review");
      //log.info("verifyVersion config " + verifyVersion);
      if (StringUtils.isNotBlank(verifyVersion)) {
        verifyVersionInt = Integer.parseInt(verifyVersion);
      }
    } catch (Exception e) {
    }
    session.setIosVerifyVersion(Client.IOS == client && (sauth.getClientVersion() >= verifyVersionInt));

    session.authed();
    session.setCountry(userDao.getCountry(sauth.getAppId(), sauth.getUserId()));
    //进房set房间模式
    session.setLiveMode(roomDao.getRoomLiveMode(sauth.getAppId(), sauth.getRoomId()));
    sessionManager.register(session);
  }

  private void deliverResponse(ClientSession clientSession, MsgBytePacket packet,PlatformUpProtos.Sauth upSauth) {
    PlatformDownProtos.SauthRet sauthMsg = PlatformDownProtos.SauthRet
        .newBuilder()
        .setMsgid(clientSession.getMsgid())
        .setMsgTime(System.currentTimeMillis())
        .setDownAesKey(clientSession.getDownAesKey())
        .build();

    MsgBytePacket result = MsgBytePackets.newSauth(packet.getHeader().getSeqId());
    result.setBody(sauthMsg.toByteArray());
    clientSession.deliverWithAuth(result);
    traceSauth(upSauth, sauthMsg, clientSession);
  }

  private void traceSauth(PlatformUpProtos.Sauth upSauth, PlatformDownProtos.SauthRet downSauth,
      ClientSession clientSession) {
    tunnel.recordSauth(upSauth, downSauth, clientSession);
  }
}

