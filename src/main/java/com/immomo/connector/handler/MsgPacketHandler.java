package com.immomo.connector.handler;

import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;

public interface MsgPacketHandler {
  void handle(MsgBytePacket packet, ClientSession session) throws Exception;
}