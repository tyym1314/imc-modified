package com.immomo.connector.protocol.v1;

import java.util.StringJoiner;

/**
 * https://moji.wemomo.com/doc#/detail/87668
 */
public class MsgBytePacketHeader {

  public static final byte Version = 1;

  /**
   * 协议版本
   */
  private byte protocolVersion = Version;

  /**
   * 消息类型
   */
  private byte msgType;
  /**
   * 客户端上传seq id
   */
  private int seqId;
  /**
   * 扩展
   */
  private byte ext;

  public byte getProtocolVersion() {
    return protocolVersion;
  }

  public void setProtocolVersion(byte protocolVersion) {
    this.protocolVersion = protocolVersion;
  }

  public byte getMsgType() {
    return msgType;
  }

  public void setMsgType(byte msgType) {
    this.msgType = msgType;
  }

  public int getSeqId() {
    return seqId;
  }

  public void setSeqId(int seqId) {
    this.seqId = seqId;
  }

  public byte getExt() {
    return ext;
  }

  public void setExt(byte ext) {
    this.ext = ext;
  }

  public static int length() {
    return 7;
  }

  @Override
  public String toString() {
    return new StringJoiner(", ", MsgBytePacketHeader.class.getSimpleName() + "[", "]")
        .add("protocolVersion=" + protocolVersion)
        .add("msgType=" + msgType)
        .add("seqId=" + seqId)
        .add("ext=" + ext)
        .toString();
  }


  public static boolean isCompress(byte ext) {
    return (ext & 0x01) != 0;
  }

  public static byte setCompress(byte ext) {
    return (byte) (ext | 0x01);
  }
}
