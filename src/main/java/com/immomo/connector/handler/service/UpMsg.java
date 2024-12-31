package com.immomo.connector.handler.service;

/**
 * Created By wlb on 2019-11-21 12:28
 */
//https://moji.wemomo.com/doc#/detail/92191
public class UpMsg {

  private String userId;
  private String roomId;
  private String linkChannelId;
  private byte[] upData;

  public UpMsg(String userId, String roomId, String linkChannelId, byte[] upData) {
    this.userId = userId;
    this.roomId = roomId;
    this.linkChannelId = linkChannelId;
    this.upData = upData;
  }

  public UpMsg() {
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
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

  public byte[] getUpData() {
    return upData;
  }

  public void setUpData(byte[] upData) {
    this.upData = upData;
  }
}
