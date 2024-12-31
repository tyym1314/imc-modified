package com.immomo.connector.protocol.v1;

import java.util.Arrays;

public class MsgBytePacket {

  private MsgBytePacketHeader header;
  private byte[] body = new byte[0];
  /**
   *   deliver类型 如果类型是1就是session加密方式
   *   如果类型是2就是group加密方式，默认是1
   */
  private int deliverType = 1;

  public MsgBytePacket() {
  }

  public MsgBytePacket(MsgBytePacketHeader header, byte[] body) {
    this.header = header;
    this.body = body;
  }

  public byte[] getBody() {
    return body;
  }

  public void setBody(byte[] body) {
    this.body = body;
  }

  public int getDeliverType() {
    return deliverType;
  }

  public void setDeliverType(int deliverType) {
    this.deliverType = deliverType;
  }

  public MsgBytePacketHeader getHeader() {
    return header;
  }

  public void setHeader(MsgBytePacketHeader header) {
    this.header = header;
  }

  @Override
  public String toString() {
    return "MsgBytePacket [header=" + header + ", body=" + Arrays.toString(body) + "]";
  }

  public int length() {
    int headerLength = header != null ? MsgBytePacketHeader.length() : 0;
    int bodyLength = body != null ? body.length : 0;
    return headerLength + bodyLength;
  }
}
