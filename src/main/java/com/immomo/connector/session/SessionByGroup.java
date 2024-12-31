package com.immomo.connector.session;

import com.immomo.connector.bean.MsgSource;
import com.immomo.connector.decrypt.MsgEncrypter;
import com.immomo.connector.decrypt.impl.LibCodedMsgEncrypter;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.mcf.util.encrypt.Base64;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelId;
import io.netty.util.AttributeKey;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomUtils;

/**
 * 按roomId组织session
 *
 * Created By wlb on 2019-09-23 15:43
 */
@Slf4j
public class SessionByGroup {

  private static final AttributeKey<SessionByGroup> sessionGroup = AttributeKey
      .newInstance("sessionGroup");

  /*roomid*/
  private final String groupId;
  private final ConnectionGroup group;
  private final String aesKey;

  private final MsgEncrypter msgEncrypter;

  public SessionByGroup(String groupId) {
    this.groupId = groupId;
    this.group = new ConnectionGroup();
    this.aesKey = Base64.encodeBytes(RandomUtils.nextBytes(15));
    this.msgEncrypter = new LibCodedMsgEncrypter();
  }

  public String groupId() {
    return groupId;
  }

  public String aesKey() {
    return aesKey;
  }

  public void addSession(ClientSession session) {
    Channel channel = session.getChannel();
    group.add(channel);
    channel.attr(sessionGroup).set(this);
  }

  public void deliver(MsgBytePacket packet,
      Predicate<ClientSession> sessionPredicate) {
    HubbleUtils.incrCount("down_deliver_msg_req");

    MsgBytePacket encrypt = msgEncrypter.encrypt(packet, aesKey);
    group.writeAndFlush(encrypt, sessionPredicate);
    HubbleUtils.incrCount("down_deliver_msg_succ");
  }


  public void deliverBatch(MsgBytePacket packet,
                           Predicate<ClientSession> sessionPredicate) {
    Collection<Channel> channels = group.getChannels();
    for (Channel channel : channels) {
      if (channel.isActive() && channel.isRegistered()) {
        ClientSession session = SessionManager.get(channel);
        if (!sessionPredicate.test(session)) {
          continue;
        }
        session.deliver(packet, MsgSource.ROOM);
      }
    }
  }
    
  public List<ClientSession>  getSessions() {
    return group.getChannels().stream().filter(Objects::nonNull).filter(channel -> channel.isActive() && channel.isRegistered()).map(SessionManager::get).collect(Collectors.toList());
  }




  private static class ConnectionGroup {

    private final ChannelFutureListener remover = future -> remove(future.channel());

    private final ConcurrentMap<ChannelId, Channel> channels;

    public ConnectionGroup() {
      this.channels = new ConcurrentHashMap<>();
    }

    public  Collection<Channel> getChannels() {
      return this.channels.values();
    }

    //!!! packet内容不能变
    public void writeAndFlush(final MsgBytePacket packet,
        Predicate<ClientSession> sessionPredicate) {
      Collection<Channel> channels = this.channels.values();
      for (Channel channel : channels) {
        if (channel.isActive() && channel.isRegistered()) {
          ClientSession session = SessionManager.get(channel);
          if (!sessionPredicate.test(session)) {
            continue;
          }
          channel.writeAndFlush(packet).addListener(future -> {
            if (!future.isSuccess()) {
              //HubbleUtils.incrCount("down_write_flush_fail");
              HubbleUtils.incrCount("room_msg_flush_fail");
              log.error("roomMsgFlushFail:{}", session.toString());
            }
          });

          HubbleUtils.incrCountByClient("room_msg_flush_total", session.getClient());
          //HubbleUtils.incrCountByClient("down_write_flush_total", session.getClient());
        } else {
          HubbleUtils.incrCount("room_bad_active");
        }
      }
    }

    public void add(Channel channel) {
      boolean added = channels.putIfAbsent(channel.id(), channel) == null;
      if (added) {
        channel.closeFuture().addListener(remover);
      }
    }

    public void remove(Channel channel) {
      Channel c = channels.remove(channel.id());
      if (c != null) {
        c.closeFuture().removeListener(remover);
      }
    }

    public Collection<Channel> channels() {
      return channels.values();
    }

    public int size() {
      return channels.size();
    }
  }
}
