package com.immomo.connector.config;

/**
 * Created By wlb on 2019-11-27 18:48
 */
public class Config {

  private String pubsubCluster;

  private String zkAddress;

  private String kafkaAddress;

  private String userCluster;


  private String roomRedis;


  public String getPubsubCluster() {
    return pubsubCluster;
  }

  public void setPubsubCluster(String pubsubCluster) {
    this.pubsubCluster = pubsubCluster;
  }

  public String getZkAddress() {
    return zkAddress;
  }

  public void setZkAddress(String zkAddress) {
    this.zkAddress = zkAddress;
  }

  public String getKafkaAddress() {
    return kafkaAddress;
  }

  public void setKafkaAddress(String kafkaAddress) {
    this.kafkaAddress = kafkaAddress;
  }

  public String getUserCluster() {
    return userCluster;
  }

  public void setUserCluster(String userCluster) {
    this.userCluster = userCluster;
  }


  public String getRoomRedis() {
    return roomRedis;
  }

  public void setRoomRedis(String roomRedis) {
    this.roomRedis = roomRedis;
  }

}
