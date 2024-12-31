package com.immomo.connector.handler.component;

import com.immomo.goback3.api.broker.GobackBrokerService;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created By wlb on 2019-11-15 14:37
 */
@Component
public class GobackProducer {

  @Autowired
  private GobackBrokerService gobackBrokerService;

  public boolean publish(String topicGroup, String topic, String channel, String body,
      Map<String, Object> config) {
    return gobackBrokerService.publish(topicGroup, topic, channel, body, config);
  }

  public boolean submitByGroup(String topicGroup, String topic, String body, String channel,
      Map<String, Object> config) {
    return gobackBrokerService.submitByGroup(topicGroup, topic, body, channel, config);
  }

  public boolean submit(String topic, String body, String channel, Map<String, Object> config) {
    return gobackBrokerService.submit(topic, body, channel, config);
  }

}
