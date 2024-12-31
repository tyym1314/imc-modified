package com.immomo.connector.protocol;

import com.immomo.connector.exceptions.BadMsgBytePacketException;
import com.immomo.connector.protocol.model.ProtocolToken;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.protocol.v1.MsgBytePacketHeader;
import com.immomo.connector.session.CAUSES;
import com.immomo.live.im.connector.bean.PlatformDownProtos;
import io.netty.buffer.ByteBuf;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MsgBytePackets {

  public static MsgBytePacket newPong(int seqid) {
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.PONG);
    header.setSeqId(seqid);
    MsgBytePacket p = new MsgBytePacket();
    p.setHeader(header);
    return p;
  }

  public static MsgBytePacket newRet(int seqid) {
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.RET);
    header.setSeqId(seqid);
    MsgBytePacket p = new MsgBytePacket();
    p.setHeader(header);
    return p;
  }

  /**
   * new success Ret
   */
  public static MsgBytePacket newSuccessRet(int seqid, String msgid) {
    MsgBytePacket packet = newRet(seqid);
    PlatformDownProtos.RetMsg retMsg = PlatformDownProtos.RetMsg.newBuilder()
        .setEc(CAUSES.SUCCESS.EC)
        .setEm(CAUSES.SUCCESS.EM)
        .setMsgid(msgid)
        .setMsgTime(System.currentTimeMillis()).build();
    packet.setBody(retMsg.toByteArray());
    return packet;
  }

  public static MsgBytePacket newPacket(byte msgType, int seqid, byte[] body) {
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(msgType);
    header.setSeqId(seqid);
    MsgBytePacket p = new MsgBytePacket();
    p.setHeader(header);
    p.setBody(body);
    return p;
  }


  /**
   * 解包
   */
  public static MsgBytePacket unPacket(ByteBuf msg) {
    int totalLength = msg.readableBytes();
    if (totalLength < MsgBytePacketHeader.length()) {
      throw new BadMsgBytePacketException("illegal packet length:" + totalLength);
    }

    MsgBytePacket packet = new MsgBytePacket();

    byte clientProtocolVersion = msg.readByte();
    if (clientProtocolVersion != MsgBytePacketHeader.Version) {
      throw new BadMsgBytePacketException("illegal protocol version:" + clientProtocolVersion);
    }

    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(msg.readByte());
    header.setSeqId(msg.readInt());
    header.setExt(msg.readByte());

    packet.setHeader(header);

    int bodyLength = totalLength - MsgBytePacketHeader.length();
    if (bodyLength > 0) {
      byte[] body = new byte[bodyLength];
      msg.readBytes(body);
      packet.setBody(body);
    }

    return packet;
  }

  /**
   * 编码包
   */
  public static void packet(MsgBytePacket msg, ByteBuf out) {
    MsgBytePacketHeader header = msg.getHeader();
    out.writeByte(header.getProtocolVersion());
    out.writeByte(header.getMsgType());
    out.writeInt(header.getSeqId());
    out.writeByte(header.getExt());
    if (msg.getBody() != null) {
      out.writeBytes(msg.getBody());
    }
  }

  public static MsgBytePacket newSauth(int seqid) {
    MsgBytePacket packet = new MsgBytePacket();
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.SAUTH_RET);
    header.setSeqId(seqid);
    packet.setHeader(header);
    return packet;
  }

  public static MsgBytePacketHeader newDownMsgHeader() {
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.DOWN_BIZ_MSG);
    header.setSeqId(0);
    return header;
  }

  public static MsgBytePacketHeader newDownLinkHeader() {
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.LINK_DOWN);
    header.setSeqId(0);
    return header;
  }

  public static MsgBytePacket newKick(byte[] body) {
    MsgBytePacket packet = new MsgBytePacket();
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.KICK);
    header.setSeqId(0);
    packet.setHeader(header);
    packet.setBody(body);
    return packet;
  }

  public static MsgBytePacket newReconn(byte[] body) {
    MsgBytePacket packet = new MsgBytePacket();
    MsgBytePacketHeader header = new MsgBytePacketHeader();
    header.setMsgType(ProtocolToken.RECONN);
    header.setSeqId(0);
    packet.setHeader(header);
    packet.setBody(body);
    return packet;
  }
}
