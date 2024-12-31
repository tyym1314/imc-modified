package com.immomo.connector.tunnel;

/**
 * @Author: Jacklin
 * @Date: 2020/5/22 3:17 下午
 */
public class TunnelData {

  /**
   * 时间
   */
  private long time;

  /**
   * 服务端ip
   */
  private String server_ip;

  /**
   * 客户端ip
   */
  private String client_ip;

  /**
   * 上行 or 下行
   */
  private TunnelDataType type;

  /**
   * 协议内容
   */
  private String content;

  private String msgType;

  private byte[] contentPb;

  public byte[] getContentPb() {
    return contentPb;
  }

  public void setContentPb(byte[] contentPb) {
    this.contentPb = contentPb;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

  public String getServer_ip() {
    return server_ip;
  }

  public void setServer_ip(String server_ip) {
    this.server_ip = server_ip;
  }

  public String getClient_ip() {
    return client_ip;
  }

  public void setClient_ip(String client_ip) {
    this.client_ip = client_ip;
  }

  public TunnelDataType getType() {
    return type;
  }

  public void setType(TunnelDataType type) {
    this.type = type;
  }

  public String getContent() {
    return content;
  }

  public void setContent(String content) {
    this.content = content;
  }

  public String getMsgType() {
    return msgType;
  }

  public void setMsgType(String msgType) {
    this.msgType = msgType;
  }

}
