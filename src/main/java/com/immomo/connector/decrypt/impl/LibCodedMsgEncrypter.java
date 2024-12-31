package com.immomo.connector.decrypt.impl;

import com.immomo.connector.decrypt.MsgEncrypter;
import com.immomo.connector.protocol.v1.MsgBytePacket;

/**
 * Created By wlb on 2019-10-22 16:36
 */
public class LibCodedMsgEncrypter implements MsgEncrypter {

  @Override
  public MsgBytePacket encrypt(MsgBytePacket packet, String encryptKey) {
    if (packet.getBody() == null || packet.getBody().length == 0) {
      return packet;
    }
    return new MsgBytePacket(packet.getHeader(), LiveAESV3.encode(packet.getBody(), encryptKey));
  }
}