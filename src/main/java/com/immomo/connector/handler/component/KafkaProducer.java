package com.immomo.connector.handler.component;

import com.immomo.connector.config.Config;
import com.immomo.connector.config.ProjectConfig;
import com.immomo.connector.util.ImIpUtils;
import com.immomo.kafka.MomoKafkaClient;
import com.immomo.kafka.producer.Callback;
import com.immomo.kafka.producer.MomoKafkaProducer;
import com.immomo.kafka.producer.ProducerConfig;
import com.immomo.kafka.serializer.Serializer;
import com.immomo.kafka.serializer.SerializerImpl;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Created By wlb on 2019-11-27 19:27
 */
@Slf4j
@Component
public class KafkaProducer implements InitializingBean, DisposableBean {

  @Autowired
  private Config config;

  private MomoKafkaProducer<String, String> producer;

  @Override
  public void afterPropertiesSet() throws Exception {
    ProducerConfig configs = new ProducerConfig(config.getKafkaAddress());
    Serializer stringSerializer = SerializerImpl.getStringSerializer();
    configs.setKeySerializer(stringSerializer);
    configs.setValueSerializer(stringSerializer);
    configs.setAcks("1");
    configs.setBatchSize(16384);
    configs.setLingerMs(1000);
    configs.setMaxInflightRequestPerConnection(5);
    configs.put("compression.type", "none");
    configs.put("client.id", ProjectConfig.PROJECT_NAME + "@" + ImIpUtils.HOST_NAME);
    //因为要先关闭链接在关闭producer 所有不要使用ShutdownHook，因为ShutdownHook没有顺序
    configs.setEnableShutdownHookClose(false);
    producer = MomoKafkaClient.createProducer(configs);
  }

  @Override
  public void destroy() throws Exception {
    if (producer != null) {
      log.info("DefaultMomoKafkaProducer closed start");
      producer.close(5, TimeUnit.SECONDS);
    }
  }

  public void sendAsync(String topic, String key, String msg) {
    try {
      producer.sendAsync(topic, key, msg, callback);
    } catch (Exception e) {
      log.error("sendAsync error " + ExceptionUtils.getStackTrace(e));
    }
  }


  private static final Callback callback = (s, e) -> {
    if (e != null) {
      log.error("kafka sendAsync error " + ExceptionUtils.getStackTrace(e));
    }
  };
}
