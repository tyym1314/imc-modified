package com.immomo.connector.handler;

import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.protocol.v1.MsgBytePacketHeader;
import com.immomo.connector.session.ClientSession;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.xerial.snappy.Snappy;

/**
 * Created By wlb on 2019-10-22 15:51
 */
@Slf4j
@Component
public class UnCompressHandler implements MsgPacketHandler {

  @Override
  public void handle(MsgBytePacket packet, ClientSession session) throws Exception {
    if (packet.getBody() == null || packet.getBody().length == 0) {
      return;
    }

    byte ext = packet.getHeader().getExt();
    if (!MsgBytePacketHeader.isCompress(ext)) {
      return;
    }

    byte msgType = packet.getHeader().getMsgType();
    try {
      byte[] uncompress = Snappy.uncompress(packet.getBody());
      packet.setBody(uncompress);
    } catch (IOException e) {
      throw new IllegalStateException("Failed to uncompress body, msgType:" + msgType, e);
    }
  }


}
