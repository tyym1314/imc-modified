package com.immomo.connector.session;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.immomo.connector.constants.Constants;
import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.util.MsgIdUtils;
import com.immomo.live.im.connector.bean.PlatformDownProtos.Kick;
import java.util.Collection;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;

/**
 * 按appId组织Session
 *
 * Created By wlb on 2019-09-25 18:47
 */
@Slf4j
public class SessionByApplication {

  private final String appId;

  private final ConcurrentMap<String/*userId*/, ClientSession> sessions = new ConcurrentHashMap<>();

  private final LoadingCache<String/* roomId */, SessionByGroup> groupSessions = CacheBuilder
      .newBuilder()
      .weakValues()
      .build(new CacheLoader<String, SessionByGroup>() {
        @Override
        public SessionByGroup load(String key) throws Exception {
          return new SessionByGroup(key);
        }
      });

  public SessionByApplication(String appId) {
    this.appId = appId;
  }

  public String appId() {
    return appId;
  }

  public void register(ClientSession session) {
    String userId = session.getUserId();

    ClientSession oldSession = sessions.put(userId, session);
    if (oldSession != null && !oldSession.getChannel().id().equals(session.getChannel().id())) {
      oldSession.setNewRoomId(session.getRoomId());

      byte[] bytes = Kick.newBuilder().setMsgid(MsgIdUtils.id())
          .setMsgTime(System.currentTimeMillis()).build().toByteArray();
      MsgBytePacket packet = MsgBytePackets.newKick(bytes);
      oldSession.deliverAndClose(packet, Constants.CloseReason_Kick);
    }

    //group
    SessionByGroup sessionGroup = getGroup(session.getRoomId());
    sessionGroup.addSession(session);

    //set aes key
    session.setDownAesKey(sessionGroup.aesKey());
  }

  public void unRegister(ClientSession session) {
    String userId = session.getUserId();
    sessions.remove(userId, session);
  }

  public ClientSession get(String userId) {
    return sessions.get(userId);
  }

  public SessionByGroup getGroup(String roomId) {
    return groupSessions.getUnchecked(roomId);
  }

  public ConcurrentMap<String, SessionByGroup> getAllGroups() {
    return groupSessions.asMap();
  }

  public Collection<ClientSession> getAllSessions() {
    return sessions.values();
  }
}
