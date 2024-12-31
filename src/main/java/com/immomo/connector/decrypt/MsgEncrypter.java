package com.immomo.connector.decrypt;

import com.immomo.connector.protocol.v1.MsgBytePacket;

/**
 * Created By wlb on 2019-10-22 16:33
 */
public interface MsgEncrypter {

  MsgBytePacket encrypt(MsgBytePacket packet, String encryptKey);

}