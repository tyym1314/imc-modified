package com.immomo.connector.handler.service.impl;

import com.immomo.connector.handler.component.GobackProducer;
import com.immomo.connector.handler.component.IConfig;
import com.immomo.connector.handler.service.UpMsg;
import com.immomo.connector.handler.service.UpMsgDispatcher;
import com.immomo.mcf.util.JsonUtils;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.codehaus.jackson.type.TypeReference;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Created By wlb on 2019-11-21 12:25
 */
@Slf4j
@Service
public class UpMsgDispatcherImpl implements UpMsgDispatcher, InitializingBean {

  @Autowired
  private GobackProducer gobackComponent;

  @Autowired
  private IConfig config;

  private volatile Map<String, AppNotify> appNotifyMap = new HashMap<>();

  @Override
  public void dispatch(String appId, UpMsg upMsg) {
    AppNotify appNotify = appNotifyMap.get(appId);
    if (appNotify == null) {
      log.warn("appNotify config not found for appid:{}", appId);
      return;
    }

    try {
      gobackComponent.submitByGroup(appNotify.getTopicGroup(), "event-live-im-platform",
          JsonUtils.toJSON(upMsg), appNotify.getCallback(), new HashMap<>());
    } catch (Exception e) {
      log.error("error submitint to goback:{}", ExceptionUtils.getMessage(e), e);
    }
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    String configVal = this.config.getConfigWithDefaultValue("app_msg_notify", "{}");
    reloadAppNotify(configVal);
    this.config.addNodeListener("app_msg_notify", (event, path, data) -> reloadAppNotify(data));
  }

  private void reloadAppNotify(String configVal) {
    Map<String, AppNotify> map = JsonUtils
        .toT(configVal, new TypeReference<Map<String, AppNotify>>() {
        });
    if (map == null) {
      log.error("Failed to AppNotify");
      return;
    }
    appNotifyMap = map;
  }

  static class AppNotify {

    private String topicGroup;
    private String callback;

    public String getTopicGroup() {
      return topicGroup;
    }

    public void setTopicGroup(String topicGroup) {
      this.topicGroup = topicGroup;
    }

    public String getCallback() {
      return callback;
    }

    public void setCallback(String callback) {
      this.callback = callback;
    }
  }
}