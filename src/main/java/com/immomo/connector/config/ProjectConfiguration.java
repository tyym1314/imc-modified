package com.immomo.connector.config;

import com.immomo.mcf.util.JsonUtilsV2;
import io.netty.util.internal.SystemPropertyUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.core.env.Environment;

import java.util.Properties;

/**
 * Created By wlb on 2019-11-27 18:47
 */
@Configuration
@PropertySource("classpath:config.properties")
@Slf4j
public class ProjectConfiguration {




  @Autowired
  private Environment environment;


  @Bean
  public Config redisSubConfig() throws RuntimeException {
    String env =  SystemPropertyUtil.get("boot.env", "");
    if (StringUtils.isEmpty(env)) {
      throw new RuntimeException("cant found env! start fail!");
    }
    String pubRedis =  environment.getProperty("redis.sub.cluster." + env);
    String zookeeperAddr = environment.getProperty("connector.zookeeper.address." + env);
    String kafkaAddress = environment.getProperty("kafka.address." + env);
    String userRedis = environment.getProperty("redis.user.cluster." + env);
    String roomRedis = environment.getProperty("redis.room.cluster." + env);


    log.info("boot.env:{}, pubRedis:{}, zookeeperAddr:{}, kakfaAddr:{}, userRedis:{}, roomRedis:{}", env, pubRedis, zookeeperAddr, kafkaAddress, userRedis, roomRedis);
    Config config = new Config();
    config.setPubsubCluster(pubRedis);
    config.setZkAddress(zookeeperAddr);
    config.setKafkaAddress(kafkaAddress);
    config.setUserCluster(userRedis);
    config.setRoomRedis(roomRedis);
    return config;
  }

}
