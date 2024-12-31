package com.immomo.connector.tunnel;

import com.google.common.collect.Sets;
import com.googlecode.protobuf.format.JsonFormat;
import com.immomo.connector.decrypt.MsgDecrypter;
import com.immomo.connector.decrypt.impl.LibCodedMsgDecrypter;
import com.immomo.connector.protocol.model.ProtocolToken;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.session.SessionByGroup;
import com.immomo.connector.session.SessionManager;
import com.immomo.connector.util.BitsUtils;
import com.immomo.connector.util.ImIpUtils;
import com.immomo.live.im.connector.bean.PlatformDownProtos;
import com.immomo.live.im.connector.bean.PlatformUpProtos;
import com.immomo.live.im.connector.bean.PlatformUpProtos.ClientMsg;
import com.immomo.mcf.util.JsonUtils;
import com.immomo.mcf.util.StringUtils;
import io.netty.channel.ChannelHandlerContext;
import java.util.Set;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

/**
 * @author yang.zhaocheng@immomo.com
 * @description
 * @data 2022/01/25 下午6:23
 **/
@Slf4j
@Component
public class Tunnel implements InitializingBean {

  private final static Logger LOG = LoggerFactory.getLogger(Tunnel.class);

  private ScheduledThreadPoolExecutor task = new ScheduledThreadPoolExecutor(1);

//  private IStoreDao tunnelDao = null;

  @Resource
  private ImProxyTunnelService imProxyTunnelService;

  private static final String prefix = "sc:";

  private static final String momoidKeyTemplate = prefix + "momoid:%s:tunnel";

  private static final String hashKey = "1";

  private static final String listKey = prefix + "tunnellist";

  private static final String momoidPubSubKeyTemplate = prefix + "momoid:%s:pubsub";

  private static volatile Set<String> momoids = Sets.newHashSet();


  private MsgDecrypter msgDecrypter = new LibCodedMsgDecrypter();

  @Resource
  private SessionManager sessionManager;


  /**
   * 是否命中 tunnel
   */
  private boolean isHit(String momoid) {
    return StringUtils.isNotBlank(momoid) && momoids.contains(momoid);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    this.start();
  }

  public void start() {
    task.scheduleAtFixedRate(() -> {
      try {
        Set<String> momoidsTmp = imProxyTunnelService.getTunnelMomoids();
        momoids = Sets.filter(momoidsTmp, momoid -> {
          boolean exists = imProxyTunnelService.existTunnelMomoid(momoid);
          if (!exists) {
            imProxyTunnelService.delTunnelMomoid(momoid);
          }
          return exists;
        });
        if (CollectionUtils.isNotEmpty(momoids)) {
          LOG.info("tunnelV2 scan momoid : {}", JsonUtils.toJSON(momoids));
        }

      } catch (Exception e) {
        e.printStackTrace();
        LOG.error("tunnelV2 error " + e.getMessage());
      }
    }, 0, 10, TimeUnit.SECONDS);
  }

  public void recordTunnel(ChannelHandlerContext ctx, MsgBytePacket msg) {
    ClientSession clientSession = sessionManager.get(ctx.channel());
    if (clientSession != null) {
      //TODO 监控
//      CounterMonitors.incMessageOut(clientSession.getRoomid(), clientSession.getClientRole());
//      MsgOutMonitorManager.MSG_OUT_COUNTER.inc();
      try {
        recordDown(clientSession, msg);
      } catch (Exception e) {
        LOG.error(e.getMessage());
      }
      return;
    }
  }


  public void recordSauth(PlatformUpProtos.Sauth upSauth, PlatformDownProtos.SauthRet downSauth,
      ClientSession session) {
    try {
      if (isHit(session.getUserId())) {
        recordUpSauth(upSauth, session);
        recordDownSauth(downSauth, session);
      }
    } catch (Exception e) {
      log.error("recordSauth error", e);
    }
  }

  public void close(ClientSession clientSession) {
    if (isHit(clientSession.getUserId())) {
      try {
        TunnelData data = productTunnelData(clientSession, TunnelDataType.Down);
        data.setMsgType("close");
        data.setContent("ClOSE");
        imProxyTunnelService.pushImData(clientSession.getUserId(),
            JsonUtils.toJSON(data));
      } catch (Exception e) {
        LOG.error("close error " + ExceptionUtils.getStackTrace(e));
      }
    }
  }


  public void recordPing(ClientSession clientSession) {
    if (isHit(clientSession.getUserId())) {
      recordUp(clientSession, "UpPing", "PING");
    }
  }

  /**
   * 记录上行 sauth
   */
  private void recordUpSauth(PlatformUpProtos.Sauth sauth, ClientSession clientSession) {
    recordUp(clientSession, "UpSauth", JsonFormat.printToString(sauth));
  }

  /**
   * 记录下行 sauth
   */
  private void recordDownSauth(PlatformDownProtos.SauthRet sauth, ClientSession clientSession) {
    try {
      TunnelData data = productTunnelData(clientSession, TunnelDataType.Down);
      data.setMsgType("sauth");
      data.setContent(JsonFormat.printToString(sauth));
      imProxyTunnelService.pushImData(clientSession.getUserId(),
          JsonUtils.toJSON(data));
    } catch (Exception e) {
      LOG.error("recordDownSauth error " + ExceptionUtils.getStackTrace(e));
    }
  }

  /**
   * 记录上行聊聊
   */
  public void recordUpBili(PlatformUpProtos.ClientMsg bili, ClientSession clientSession) {
    if (isHit(clientSession.getUserId())) {
      recordUp(clientSession, "UpBili", JsonFormat.printToString(bili));
    }
  }

  /**
   * 记录下行
   */
  public void recordDown(ClientSession clientSession, MsgBytePacket packet) {
    if (!isHit(clientSession.getUserId())) {
      return;
    }
    try {
      TunnelData data = productTunnelData(clientSession, TunnelDataType.Down);

      boolean isCompressMsg = BitsUtils.isSet(packet.getHeader().getExt(), 1);

      if (packet.getHeader().getMsgType() == ProtocolToken.DOWN_BIZ_MSG ||
      packet.getHeader().getMsgType() == ProtocolToken.LINK_DOWN) {
        MsgBytePacket decrypt = null;
        if (packet.getDeliverType() == 1){
          decrypt = msgDecrypter.decryptToNewPacket(packet, clientSession.getDownAesKey());
        }else{
          SessionByGroup sessionByGroup = sessionManager.getGroup(clientSession.getAppId(), clientSession.getRoomId());
          if (sessionByGroup != null) {
            decrypt = msgDecrypter.decryptToNewPacket(packet, sessionByGroup.aesKey());
            log.info("sessionByGroup msgType {} decryptToNewPacket decrypt {}", packet.getHeader().getMsgType(), JsonUtils.toJSON(decrypt));
          }
        }
        //两次解密不出来说明有问题打错误日志
        if (decrypt == null) {
          log.error("tunnel decrypt packet fail session {} packet", clientSession, packet);
          return;
        }
        data.setContentPb(decrypt.getBody());
        data.setMsgType(packet.getHeader().getMsgType() == ProtocolToken.DOWN_BIZ_MSG ?
            "DOWN_BIZ_MSG" : "LINK_DOWN");
        imProxyTunnelService.pushImData(clientSession.getUserId(),
            JsonUtils.toJSON(data));
        return;
      } else if (packet.getHeader().getMsgType() == ProtocolToken.PONG) {
        data.setContent("PONG");
        data.setMsgType("pong");
      } else if (packet.getHeader().getMsgType() == ProtocolToken.RET) {
        MsgBytePacket decrypt = msgDecrypter.decryptToNewPacket(packet, clientSession.getDownAesKey());
        data.setMsgType("ret");
        data.setContentPb(decrypt.getBody());
      } else if (packet.getHeader().getMsgType() == ProtocolToken.KICK) {
        MsgBytePacket decrypt = msgDecrypter.decryptToNewPacket(packet, clientSession.getDownAesKey());
        data.setContentPb(decrypt.getBody());
        data.setMsgType("kick");
      } else if (packet.getHeader().getMsgType() == ProtocolToken.RECONN) {
        MsgBytePacket decrypt = msgDecrypter.decryptToNewPacket(packet, clientSession.getDownAesKey());
        data.setContentPb(decrypt.getBody());
        data.setMsgType("reconn");
      } else {
        return;
      }
      imProxyTunnelService.pushImData(clientSession.getUserId(),
          JsonUtils.toJSON(data));

    } catch (Exception e) {
      LOG.error("recordDown error " + ExceptionUtils.getStackTrace(e));
    }
  }


  private void recordUp(ClientSession clientSession, String msgType, String content) {
    try {
      TunnelData data = productTunnelData(clientSession, TunnelDataType.Up);
      data.setMsgType(msgType);
      data.setContent(content);
      imProxyTunnelService.pushImData(clientSession.getUserId(),
          JsonUtils.toJSON(data));
    } catch (Exception e) {
      LOG.error("recordUp error " + ExceptionUtils.getStackTrace(e));
    }
  }

  public void recordUp(ClientSession clientSession, String msgType, ClientMsg clientMsg) {
    if (!isHit(clientSession.getUserId())) {
      return;
    }
    try {
      TunnelData data = productTunnelData(clientSession, TunnelDataType.Up);
      data.setMsgType(msgType);
      data.setContentPb(clientMsg.getData().toByteArray());
      log.info("recordUp session {} msgType {} clientMsg {}", JsonUtils.toJSON(clientSession),
          msgType, JsonFormat.printToString(clientMsg));
      imProxyTunnelService.pushImData(clientSession.getUserId(),
          JsonUtils.toJSON(data));
    } catch (Exception e) {
      LOG.error("recordUp error " + ExceptionUtils.getStackTrace(e));
    }
  }

  private static TunnelData productTunnelData(ClientSession clientSession, TunnelDataType type) {
    TunnelData data = new TunnelData();
    data.setTime(System.currentTimeMillis());
    data.setServer_ip(ImIpUtils.IP_LAN);
    data.setClient_ip(clientSession.getAddress());
    data.setType(type);
    return data;
  }


}
