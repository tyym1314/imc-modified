package com.immomo.connector.session;

import com.immomo.connector.bean.MsgSource;
import com.immomo.connector.decrypt.MsgEncrypter;
import com.immomo.connector.decrypt.impl.LibCodedMsgEncrypter;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.util.MsgIdUtils;
import com.immomo.connector.util.config.ConfigUtil;
import com.immomo.env.MomoEnv;
import io.netty.channel.Channel;
import java.net.SocketAddress;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ClientSession {

  private final AtomicBoolean sauthed = new AtomicBoolean(false);
  private final Channel channel;
  private final String address;

  private Client client;
  private int clientVersion;

  private String appId;
  private String userId;
  private String roomId;
  private String linkChannelId;
  private String deviceId;
  private String ua;
  private String src;
  private int netType;

  private String upAesKey;
  private String downAesKey;

  private long sauthTime;
  private AtomicLong lastPingTs = new AtomicLong();

  private int closeReason;
  //被踢时，新的roomid
  private String newRoomId;

  // 是否是ios审核版本
  private boolean iosVerifyVersion = false;

  // 用户地区
  private String country;


  private int liveMode;



  private MsgEncrypter msgEncrypter = new LibCodedMsgEncrypter();

  public ClientSession(Channel channel) {
    this.channel = channel;
    SocketAddress address = channel.remoteAddress();
    this.address = address.toString().substring(1);
  }

  public void deliver(MsgBytePacket bytePacket, MsgSource source) {
    if (!channel.isActive() || !channel.isRegistered()) {
      HubbleUtils.incrCount(source + "_bad_active");
      HubbleUtils.incrCount( "bad_active");
      return;
    }
    MsgBytePacket encrypt = msgEncrypter.encrypt(bytePacket, downAesKey);
    channel.writeAndFlush(encrypt).addListener(future -> {
      if (!future.isSuccess()) {
        HubbleUtils.incrCount(source + "_deliver_fail");
        HubbleUtils.incrCount("deliver_fail");
      } else {
        HubbleUtils.incrCount(source + "deliver_success");
        HubbleUtils.incrCount("deliver_success");
      }
    });
    outBoundBufferLimitMonitor(channel);
  }

  public void deliverPong(MsgBytePacket bytePacket) {
    if (!channel.isActive() || !channel.isRegistered()) {
      HubbleUtils.incrCount("ping_bad_active");
      return;
    }
    MsgBytePacket encrypt = msgEncrypter.encrypt(bytePacket, downAesKey);
    channel.writeAndFlush(encrypt).addListener(future -> {
      if (!future.isSuccess()) {
        ClientSession session = SessionManager.get(channel);
        log.warn("pingDeliverFail:{}", session.toString());
        HubbleUtils.incrCount("ping_deliver_fail");
      }
    });
  }

  public void outBoundBufferLimitMonitor(Channel channel) {
    if (!channel.isWritable()) {
      long pendingWriteBytes =  channel.unsafe().outboundBuffer().totalPendingWriteBytes();
      long overBufferLimit = ConfigUtil.overOutboundBufferLimit();
      HubbleUtils.outBoundBufferMonitor("outboundbuffer_limit", pendingWriteBytes);
      if (overBufferLimit > 0 && pendingWriteBytes > overBufferLimit) {
        log.warn("io session pending WriteBytes client Session {},overBufferLimit:{},length:{}",this.toString(), overBufferLimit, pendingWriteBytes);
      }
    }
  }


  public void deliverAndClose(MsgBytePacket packet, int closeReason) {
    if (!channel.isActive()) {
      return;
    }
    MsgBytePacket encrypt = msgEncrypter.encrypt(packet, downAesKey);
    channel.writeAndFlush(encrypt).addListener(future -> this.close(closeReason));
  }

  /**
   * 还没有交换 key 时的加密返回
   */
  public void deliverWithAuth(MsgBytePacket bytePacket) {
    if (!channel.isActive() || !channel.isRegistered()) {
      HubbleUtils.incrCount("sauth_bad_active");
      return;
    }
    MsgBytePacket encrypt = msgEncrypter.encrypt(bytePacket, upAesKey);
    channel.writeAndFlush(encrypt).addListener(future -> {
      if (!future.isSuccess()) {
        ClientSession session = SessionManager.get(channel);
        log.warn("sauthDeliverFail:{}", session.toString());
        HubbleUtils.incrCount("sauth_deliver_fail");
      }
    });
  }

  /**
   * 关闭自己的连接
   */
  public void close(int closeReason) {
    this.closeReason = closeReason;
    channel.close();
  }

  public Channel getChannel() {
    return channel;
  }

  public String getAddress() {
    return address;
  }

  public Client getClient() {
    return client;
  }

  public int getClientVersion() {
    return clientVersion;
  }

  public String getAppId() {
    return appId;
  }

  public String getUserId() {
    return userId;
  }

  public String getDeviceId() {
    return deviceId;
  }

  public String getUa() {
    return ua;
  }

  public String getSrc() {
    return src;
  }

  public int getNetType() {
    return netType;
  }

  public String getUpAesKey() {
    return upAesKey;
  }

  public String getDownAesKey() {
    return downAesKey;
  }

  public long getSauthTime() {
    return sauthTime;
  }

  public int getCloseReason() {
    return closeReason;
  }

  public void setClient(Client client) {
    this.client = client;
  }

  public void setClientVersion(int clientVersion) {
    this.clientVersion = clientVersion;
  }

  public void setAppId(String appId) {
    this.appId = appId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public void setDeviceId(String deviceId) {
    this.deviceId = deviceId;
  }

  public void setUa(String ua) {
    this.ua = ua;
  }

  public void setSrc(String src) {
    this.src = src;
  }

  public void setNetType(int netType) {
    this.netType = netType;
  }

  public void setUpAesKey(String upAesKey) {
    this.upAesKey = upAesKey;
  }

  public void setDownAesKey(String downAesKey) {
    this.downAesKey = downAesKey;
  }

  public String getRoomId() {
    return roomId;
  }

  public void setRoomId(String roomId) {
    this.roomId = roomId;
  }

  public String getLinkChannelId() {
    return linkChannelId;
  }

  public void setLinkChannelId(String linkChannelId) {
    this.linkChannelId = linkChannelId;
  }

  public AtomicLong getLastPingTs() {
    return lastPingTs;
  }

  public void authed() {
    this.sauthTime = System.currentTimeMillis();
    this.sauthed.set(true);
  }

  public boolean isAuthed() {
    return sauthed.get();
  }

  public String getMsgid() {
    return MsgIdUtils.id();
  }

  public String getNewRoomId() {
    return newRoomId;
  }

  public void setNewRoomId(String newRoomId) {
    this.newRoomId = newRoomId;
  }

  public boolean isIosVerifyVersion() {
    return iosVerifyVersion;
  }

  public void setIosVerifyVersion(boolean iosVerifyVersion) {
    this.iosVerifyVersion = iosVerifyVersion;
  }

  public String getCountry() {
    return country;
  }

  public void setCountry(String country) {
    this.country = country;
  }

  public void setLiveMode(int liveMode) {
    this.liveMode = liveMode;
  }

  public int getLiveMode() {
    return liveMode;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ClientSession)) {
      return false;
    }
    ClientSession that = (ClientSession) o;
    return
        Objects.equals(getChannel().id(), that.getChannel().id()) &&
            Objects.equals(getAppId(), that.getAppId()) &&
            Objects.equals(getUserId(), that.getUserId());
  }

  @Override
  public int hashCode() {
    return Objects.hash(getChannel().id(), getAppId(), getUserId());
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", ClientSession.class.getSimpleName() + "[", "]")
        .add("sauthed=" + sauthed)
        .add("address='" + address + "'")
        .add("client=" + client)
        .add("clientVersion=" + clientVersion)
        .add("appId='" + appId + "'")
        .add("userId='" + userId + "'")
        .add("roomId='" + roomId + "'")
        .add("linkChannelId='" + linkChannelId + "'")
        .add("deviceId='" + deviceId + "'")
        .add("ua='" + ua + "'")
        .add("src='" + src + "'")
        .add("netType=" + netType)
        .add("closeReason=" + closeReason)
        .add("country=" + country)
        .toString();
  }


  /**
   * 输出日志信息
   */
  public String logInfo() {
    return "appId:" + appId + ",userId:" + userId + ",roomId:" + roomId + ",linkChannelId:"
        + linkChannelId;
  }
}
