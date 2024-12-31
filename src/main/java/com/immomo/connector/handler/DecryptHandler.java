package com.immomo.connector.handler;

import com.immomo.connector.decrypt.MsgDecrypter;
import com.immomo.connector.decrypt.impl.LibCodedMsgDecrypter;
import com.immomo.connector.protocol.model.ProtocolToken;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Created By wlb on 2019-10-22 15:33
 */
@Slf4j
@Component
public class DecryptHandler implements MsgPacketHandler {

  //TODO:动态获取？支持测试
  private MsgDecrypter msgDecrypter = new LibCodedMsgDecrypter();

  @Override
  public void handle(MsgBytePacket packet, ClientSession session) throws Exception {
    byte msgType = packet.getHeader().getMsgType();
    switch (msgType) {
      case ProtocolToken.PING:
        //NOOP
        break;
      case ProtocolToken.SAUTH:
        msgDecrypter.decryptSauth(packet, session);
        break;
      default:
        if (!session.isAuthed()) {
          throw new IllegalStateException("Msg received before auth, msgType:" + msgType);
        }
        msgDecrypter.decrypt(packet, session);
        break;
    }
  }

}
