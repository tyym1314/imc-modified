package com.immomo.connector.handler.consumer;

import com.google.common.util.concurrent.RateLimiter;
import com.google.protobuf.ByteString;
import com.immomo.connector.bean.MsgSource;
import com.immomo.connector.config.Config;
import com.immomo.connector.config.ProjectConfig.Redis;
import com.immomo.connector.constants.Constants;
import com.immomo.connector.handler.consumer.RedisSubscriber.MessageListener;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.PacketCompressor;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.Client;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.session.SessionByApplication;
import com.immomo.connector.session.SessionByGroup;
import com.immomo.connector.session.SessionManager;
import com.immomo.connector.util.MsgIdUtils;
import com.immomo.connector.util.config.ConfigUtil;
import com.immomo.env.MomoEnv;
import com.immomo.live.im.base.bean.KickMsg;
import com.immomo.live.im.base.bean.LinkMsg;
import com.immomo.live.im.base.bean.MsgFilter;
import com.immomo.live.im.base.bean.PlatMsg;
import com.immomo.live.im.base.bean.ReConnMsg;
import com.immomo.live.im.connector.bean.PlatformDownProtos;
import com.immomo.live.im.connector.bean.PlatformDownProtos.Kick;
import com.immomo.live.im.connector.bean.PlatformDownProtos.ReConn;
import com.immomo.mcf.util.JsonUtils;
import com.immomo.msc.executor.MscExecutor;

import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import io.netty.channel.Channel;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.RandomUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created By wlb on 2019-09-24 12:27
 */
@Slf4j
@Component
public class DownMsgConsumer implements InitializingBean, DisposableBean, MessageListener {

  private RedisSubscriber redisSubscriber;

  @Autowired
  private SessionManager sessionManager;

  @Autowired
  private Config redisSubConfig;

  public final static int DEFAULT_POOL_SIZE = Runtime.getRuntime().availableProcessors() * 2;
  private PacketCompressor compressor = new PacketCompressor();

  private MscExecutor mscExecutor = MscExecutor
      .newBuilder("down-msg-consumer")
      .corePoolSize(DEFAULT_POOL_SIZE)
      .maximumPoolSize(DEFAULT_POOL_SIZE)
      .workQueue(new ArrayBlockingQueue<Runnable>(1024 * 128))
      .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
      .daemon(false)
      .build();


  private MscExecutor downRoomMscExecutor = MscExecutor
          .newBuilder("down-room-msg-consumer")
          .corePoolSize(DEFAULT_POOL_SIZE)
          .maximumPoolSize(DEFAULT_POOL_SIZE)
          .workQueue(new ArrayBlockingQueue<Runnable>(1024 * 128))
          .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
          .daemon(false)
          .build();


  private MscExecutor broadcastMscExecutor = MscExecutor
          .newBuilder("broadcast-room-msg-consumer")
          .corePoolSize(DEFAULT_POOL_SIZE)
          .maximumPoolSize(DEFAULT_POOL_SIZE)
          .workQueue(new ArrayBlockingQueue<Runnable>(1024 * 128))
          .rejectedExecutionHandler(new ThreadPoolExecutor.DiscardOldestPolicy())
          .daemon(false)
          .build();



  @Override
  public void afterPropertiesSet() throws Exception {
    redisSubscriber = new RedisSubscriber(redisSubConfig.getPubsubCluster(),
        Redis.CONSUMER_CHANNELS, this);
    redisSubscriber.start();
  }

  @Override
  public void destroy() throws Exception {
    if (redisSubscriber != null) {
      redisSubscriber.stop();
    }
  }

  @Override
  public void onMessage(String channel, String message) {
    if (Redis.CHANNEL_MSG.equals(channel)) {
      PlatMsg platMsg = JsonUtils.toT(message, PlatMsg.class);
      if (Objects.isNull(platMsg)) {
        HubbleUtils.incrCount("channel_null_count");
        return;
      }
      String msgType = platMsg.getMsgType();
      MscExecutor executor = PlatMsg.MsgType_BROADCAST.equals(msgType) ? broadcastMscExecutor : downRoomMscExecutor;
      executor.execute(() -> deliverPlatMsg(message));
    } else if (Redis.CHANNEL_KICK.equals(channel)) {
      mscExecutor.execute(() -> deliverKick(message));
    } else if (Redis.CHANNEL_RECON.equals(channel)) {
      mscExecutor.execute(() -> deliverReconn(message));
    } else if (Redis.CHANNEL_LINK.equals(channel)) {
      mscExecutor.execute(() -> deliverLink(message));
    }
    HubbleUtils.incrCount(channel + "_count");
  }

  private void deliverLink(String message) {
    LinkMsg linkMsg = JsonUtils.toT(message, LinkMsg.class);
    if (linkMsg == null) {
      log.error("json to LinkMsg error, message:{}", message);
      return;
    }
    HubbleUtils.incrCount( "link_msg_req");
    HubbleUtils.addTime("link-msg-sub-delay", (System.currentTimeMillis() - linkMsg.getPubTime()));

    String appId = linkMsg.getAppId();
    String channelId = linkMsg.getChannelId();
    String msgType = linkMsg.getMsgType();
    String msgid = linkMsg.getMsgid();

    byte[] bytes = PlatformDownProtos.ServerMsg.newBuilder().setMsgid(msgid)
        .setMsgTime(System.currentTimeMillis()).setData(
            ByteString.copyFrom(linkMsg.getData())).build().toByteArray();
    MsgBytePacket packet = new MsgBytePacket(MsgBytePackets.newDownLinkHeader(), bytes);
    compressor.compress(packet);

    switch (msgType) {
      case LinkMsg.MsgType_Group:
        if (StringUtils.isEmpty(appId)) {
          HubbleUtils.incrCount("emptyAppId_" + msgType);
          return;
        }
        SessionByApplication application = sessionManager.get(appId);
        if (application == null) {
          return;
        }

        application.getAllSessions().stream()
            .filter(session -> Objects.equals(session.getLinkChannelId(), channelId))
            .forEach(session -> session.deliver(packet, MsgSource.DOWNLINKGROUP));

        break;
      case LinkMsg.MsgType_P2P:
        List<String> userIds = linkMsg.getToUserIds();
        if (CollectionUtils.isEmpty(userIds)) {
          return;
        }
        userIds.stream()
            .map(userId -> sessionManager.get(appId, userId))
            .filter(Objects::nonNull)
            .filter(session -> Objects.equals(channelId, session.getLinkChannelId()))
            .forEach(session -> {
              log.info("deliver link msg, msgid:{}, userid:{}", msgid, session.getUserId());
              session.deliver(packet, MsgSource.DOWNLINKP2P);
            });
        break;
      default:
        log.warn("not supported msgType for LinkMsg:{}", msgType);
        break;
    }
    HubbleUtils.incrCount( "link_msg_succ");
  }

  private void deliverPlatMsg(String message) {
    PlatMsg platMsg = JsonUtils.toT(message, PlatMsg.class);

    if (platMsg == null) {
      log.error("to PlatMsg error, message:{}", message);
      return;
    }
    HubbleUtils.incrCount( "down_msg_req");
    long costTime = System.currentTimeMillis() - platMsg.getPubTime();
    HubbleUtils.addTime("plat-msg-sub-delay", costTime);

    String appId = platMsg.getAppId();
    byte[] data = platMsg.getData();
    String msgid = platMsg.getMsgid();
    String msgType = platMsg.getMsgType();
    String roomId = platMsg.getRoomId();
    if (costTime >= 100) {
      HubbleUtils.incrCount("plat-msg-sub-delay200");
      log.error("deliverPlatMsgCostLongTime:{},{},{}", platMsg.getRoomId(), platMsg.getMsgType(), platMsg.getMsgid());
    }

    boolean shieldIosVerifyVersion = platMsg.isShieldIosVerifyVersion();

    byte[] bytes = PlatformDownProtos.ServerMsg.newBuilder().setMsgid(msgid)
        .setMsgTime(System.currentTimeMillis()).setData(
            ByteString.copyFrom(data)).build().toByteArray();
    MsgBytePacket packet = new MsgBytePacket(MsgBytePackets.newDownMsgHeader(), bytes);
    compressor.compress(packet);

    Predicate<ClientSession> sessionPredicate = genPredicate(platMsg);

    switch (msgType) {
      case PlatMsg.MsgType_BROADCAST:
        packet.setDeliverType(2);
        ConcurrentMap<String, SessionByGroup> allGroups = sessionManager.getAllGroups(appId);
        if (allGroups == null) {
          return;
        }

        List<ClientSession> sendSessions = sessionManager.get(appId).getAllSessions().stream().filter(Objects::nonNull).filter(sessionPredicate).collect(Collectors.toList());
        long sendSessionCount = sendSessions.size();
        //拆分发送，最多拆splitSendBroadcastTimes次
        int splitSendBroadcastTimes = ConfigUtil.splitSendBroadcastTimes();
        int batchSize = (int) sendSessionCount / splitSendBroadcastTimes;
        int rate =  NumberUtils.max(1, batchSize);
        log.info("maxSendBroadcastTimeRange value:{}, {}, {}, {}", sendSessionCount, splitSendBroadcastTimes, batchSize, rate);
        try {
          RateLimiter rateLimiter = RateLimiter.create(rate);
          sendSessions.forEach(session -> {
            rateLimiter.acquire();
            HubbleUtils.incrCount(msgType + "_msg_req");
            session.deliver(packet, MsgSource.BROADCAST);
            HubbleUtils.incrCount(msgType + "_msg_succ");
          });
        } catch (Exception e) {
          log.error("broadcaseLimter error", e);
        }
//        allGroups.values().forEach(group -> group.deliver(packet, sessionPredicate));
        break;
      case PlatMsg.MsgType_ROOM:
        packet.setDeliverType(2);
        SessionByGroup group = sessionManager.getGroup(appId, roomId);
        if (group == null) {
          return;
        }
        //HubbleUtils.incrCount(msgType + "_msg_req");
        //group.deliverBatch(packet, sessionPredicate);
        List<ClientSession> sessions = group.getSessions().stream().filter(Objects::nonNull).filter(sessionPredicate).collect(Collectors.toList());
        sessions.forEach(session -> {
          HubbleUtils.incrCount(msgType + "_msg_req");
          session.deliver(packet, MsgSource.ROOM);
          HubbleUtils.incrCount(msgType + "_msg_succ");
        });
        break;
      case PlatMsg.MsgType_PRIVATE:
        List<String> userIds = platMsg.getToUserIds();
        if (CollectionUtils.isEmpty(userIds)) {
          return;
        }
        HubbleUtils.incrCount(msgType + "_msg_req");
        userIds.stream()
            .map(userid -> sessionManager.get(appId, userid))
            .filter(Objects::nonNull)
            .filter(session -> Objects.equals(session.getRoomId(), roomId))
            .forEach(session -> {
              if (!platMsg.isShieldIosVerifyVersion() || !session.isIosVerifyVersion()) {
                //log.info("deliver msg, msgid:{}, userid:{}", msgid, session.getUserId());
                session.deliver(packet, MsgSource.PRIVATE);
                HubbleUtils.incrCountByClient("down_write_flush_single_total", session.getClient());
              }
            });
        HubbleUtils.incrCount(msgType + "_msg_succ");

        break;
      default:
        log.error("known msgType:{}", msgType);
        break;
    }
    HubbleUtils.incrCount("down_msg_succ");

  }

  private Predicate<ClientSession> genPredicate(PlatMsg platMsg) {
    MsgFilter filter = platMsg.getFilter();
    boolean alpha = "alpha".equals(MomoEnv.corp());
    // true: 不过滤 false:过滤
    return session -> {
      // ios verify version
      if (platMsg.isShieldIosVerifyVersion() && session.isIosVerifyVersion()) {
        return false;
      }

      if (filter == null) {
        return true;
      }

      // 按用户ID过滤
      List<String> userIds = filter.getUserIds();
      if (CollectionUtils.isNotEmpty(userIds) && userIds.contains(session.getUserId())) {
        return false;
      }

      // 按地区过滤
      List<String> countries = filter.getCountries();
      if (CollectionUtils.isNotEmpty(countries) && countries.contains(session.getCountry())) {
        return false;
      }

      //广播消息，过滤房间模式
      List<Integer> liveModes = filter.getLiveModes();
      if (CollectionUtils.isNotEmpty(liveModes) && liveModes.contains(session.getLiveMode())) {
        return false;
      }

      //过滤房间
      List<String> roomIds = filter.getRoomIds();
      if (CollectionUtils.isNotEmpty(roomIds) && roomIds.contains(session.getRoomId())) {
        return  false;
      }

      // 版本过滤
      Client client = session.getClient();
      int userVersion = session.getClientVersion();
      switch (client) {
        case ANDROID:
          if (filter.getAndroidVersion() > 0 && userVersion < filter.getAndroidVersion()) {
            return false;
          }
          break;
        case IOS:
          if (filter.getIosVersion() > 0 && userVersion < filter.getIosVersion()) {
            return false;
          }
          break;
        default:
          break;
      }

      boolean canDiscardMsg = ConfigUtil.canDiscardMsg();
      if (canDiscardMsg) {
        //是否可丢弃消息
        boolean canDiscard = platMsg.getCanDiscard();
        Channel channel = session.getChannel();
        if (channel == null || (canDiscard && !channel.isWritable())) {
          HubbleUtils.incrCount("discard_msg_count");
          return false;
        }
      }

      //随机丢弃比例
      int randomPercent = filter.getRandomPercent();
      //1-100
      if (RandomUtils.nextInt(1, 101) <= randomPercent) {
        if (alpha) {
          log.info("random discard msg, randomPercent:{}, userId:{}, roomId:{}, msgType:{}",
              randomPercent, session.getUserId(), session.getRoomId(), platMsg.getMsgType());
        }
        return  false;
      }

      return true;
    };
  }

  private void deliverReconn(String message) {
    ReConnMsg reConnMsg = JsonUtils.toT(message, ReConnMsg.class);
    if (reConnMsg == null) {
      log.error("to ReConnMsg error, message:{}", message);
      return;
    }
    HubbleUtils.incrCount("reconn_msg_count");
    String appId = reConnMsg.getAppId();
    String userId = reConnMsg.getUserId();

    ClientSession session = sessionManager.get(appId, userId);
    if (session != null) {
      byte[] bytes = ReConn.newBuilder().setMsgid(MsgIdUtils.id())
          .setMsgTime(System.currentTimeMillis())
          .setHost(reConnMsg.getDestIp())
          .setPort(reConnMsg.getDestPort()).build().toByteArray();
      MsgBytePacket packet = MsgBytePackets.newReconn(bytes);
      session.deliver(packet, MsgSource.OTHER);
    }
  }

  private void deliverKick(String message) {
    KickMsg kickMsg = JsonUtils.toT(message, KickMsg.class);
    if (kickMsg == null) {
      log.error("to KickMsg error, message:{}", message);
      return;
    }
    HubbleUtils.incrCount("kick_msg_count");
    String appId = kickMsg.getAppId();
    String userId = kickMsg.getUserId();
    ClientSession session = sessionManager.get(appId, userId);
    if (session != null) {
      byte[] bytes = Kick.newBuilder().setMsgid(MsgIdUtils.id())
          .setMsgTime(System.currentTimeMillis()).build().toByteArray();
      MsgBytePacket packet = MsgBytePackets.newKick(bytes);
      session.setNewRoomId(kickMsg.getRemoteRoomId());
      session.deliverAndClose(packet, Constants.CloseReason_Kick);
    }
  }
}