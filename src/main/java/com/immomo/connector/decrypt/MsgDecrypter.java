package com.immomo.connector.decrypt;

import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;

/**
 * Created By wlb on 2019-10-22 16:32
 */
public interface MsgDecrypter {

  void decryptSauth(final MsgBytePacket packet, final ClientSession session);

  void decrypt(final MsgBytePacket packet, final ClientSession session);

  MsgBytePacket decryptToNewPacket(final MsgBytePacket packet, String encryptKey);


}
