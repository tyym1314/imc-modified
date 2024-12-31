package com.immomo.connector.session;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.immomo.connector.handler.service.SessionListener;
import com.immomo.connector.monitor.HubbleUtils;
import io.netty.channel.Channel;
import java.util.concurrent.ConcurrentMap;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class SessionManager {

  private final LoadingCache<String/*appId*/, SessionByApplication> applications = CacheBuilder
      .newBuilder()
      .build(new CacheLoader<String, SessionByApplication>() {
        @Override
        public SessionByApplication load(String appId) throws Exception {
          return new SessionByApplication(appId);
        }
      });


  @Autowired
  private SessionListener sessionListener;

  public void register(ClientSession session) {
    String appId = session.getAppId();
    applications.getUnchecked(appId).register(session);
    sessionListener.onRegister(session);
    HubbleUtils.incrCount("online", 1);
  }

  public void unRegister(ClientSession session) {
    String appId = session.getAppId();
    SessionByApplication application = applications.getIfPresent(appId);
    if (application == null) {
      return;
    }
    application.unRegister(session);
    sessionListener.onUnregister(session);
    HubbleUtils.incrCount("online", -1);
  }

  public SessionByApplication get(String appId) {
    return applications.getIfPresent(appId);
  }

  public ClientSession get(String appId, String userId) {
    SessionByApplication application = applications.getIfPresent(appId);
    if (application == null) {
      return null;
    }
    return application.get(userId);
  }

  public SessionByGroup getGroup(String appId, String channelId) {
    SessionByApplication application = applications.getIfPresent(appId);
    if (application == null) {
      return null;
    }
    return application.getGroup(channelId);
  }

  public static ClientSession get(Channel channel) {
    return channel.attr(Const.CLIENTSESSION).get();
  }

  public ConcurrentMap<String, SessionByGroup> getAllGroups(String appId) {
    SessionByApplication application = applications.getIfPresent(appId);
    if (application == null) {
      return null;
    }
    return application.getAllGroups();
  }
}
