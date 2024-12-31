package com.immomo.connector.decrypt.impl;

import com.immomo.connector.confusion.ConfusionManager;
import com.immomo.connector.confusion.IConfusion;
import com.immomo.connector.decrypt.ByteArrayAsList;
import com.immomo.connector.decrypt.MsgDecrypter;
import com.immomo.connector.exceptions.DecryptException;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.util.Coded;
import com.immomo.env.MomoEnv;
import com.immomo.mcf.util.encrypt.Base64;
import lombok.extern.slf4j.Slf4j;

/**
 * Created By wlb on 2019-10-22 16:36
 */
@Slf4j
public class LibCodedMsgDecrypter implements MsgDecrypter {

  private static final int SHARED_SECRET_LENGTH = 24;
  private static final String SAUTH_ENCRYPT_KEY = "5555555";

  @Override
  public void decryptSauth(MsgBytePacket packet, ClientSession session) {
    SauthPacketReader sauthPacketReader = new SauthPacketReader(packet.getBody());
    short cPkLen = sauthPacketReader.readClientPkLength();
    byte[] b = sauthPacketReader.readClientPk(cPkLen);
    byte[] pk = LiveAESV3.decode(b, SAUTH_ENCRYPT_KEY);

    int serverPkVersion = sauthPacketReader.readServerPkVersion();

    byte[] serverSharedSecret = new byte[SHARED_SECRET_LENGTH];
    if (MomoEnv.corp().equals("alpha")) {
      String pkStr = new String(pk);
      log.info("serverSharedSecret length:{},{},{},{},{},{}", serverSharedSecret.length, pk.length,serverPkVersion,pk, pkStr, sauthPacketReader.readLuaVersion());
    }
    int r = Coded.serverSecretGen(serverSharedSecret, pk, pk.length, serverPkVersion);
    if (r < 0) {
      throw new IllegalStateException("MessageDecryptV3 serverSecretGen error ");
    }
    if (MomoEnv.corp().equals("alpha")) {
      log.info("serverSharedSecretSucc :{},{},{},{}", pk.length,serverPkVersion,pk, Base64.encodeBytes(pk));
    }
    String aesShareKeyBase64 = Base64.encodeBytes(serverSharedSecret);
    if (aesShareKeyBase64 == null || aesShareKeyBase64.length() < 4) {
      throw new IllegalStateException("invalide AesShareKeybase64 length");
    }

    int luaVersion = sauthPacketReader.readLuaVersion();
    IConfusion confusion = ConfusionManager.getV3(luaVersion);
    String finalShareKey = confusion.confusion(null, aesShareKeyBase64);

    byte[] oriBody = sauthPacketReader.readBody(cPkLen);
    try {
      byte[] realBody = LiveAESV3.decode(oriBody, finalShareKey);
      packet.setBody(realBody);
      //decrypt正常才设置key
      session.setUpAesKey(finalShareKey);
    } catch (Exception e) {
      throw new DecryptException("decrypt failed, msgType:" + packet.getHeader().getMsgType(), e);
    }
  }

  private static class SauthPacketReader {

    private byte[] body;

    public SauthPacketReader(byte[] body) {
      this.body = body;
    }

    public short readClientPkLength() {
      int index = 5;
      return (short) (body[index] << 8 | body[index + 1] & 0xFF);
    }

    public byte readClientType() {
      return body[0];
    }

    public int readClientVersion() {
      int index = 1;
      return (body[index] & 0xff) << 24 |
          (body[index + 1] & 0xff) << 16 |
          (body[index + 2] & 0xff) << 8 |
          body[index + 3] & 0xff;
    }


    public byte[] readClientPk(short clientPkLength) {
      int index = 9;
      return bytesInRange(body, index, index + clientPkLength);
    }

    public byte readServerPkVersion() {
      return body[8];
    }

    public byte readLuaVersion() {
      return body[7];
    }

    public byte[] readBody(short clientPkLength) {
      return bytesInRange(body, 9 + clientPkLength, body.length);
    }
  }

  private static byte[] bytesInRange(byte[] source, int start, int end) {
    return ByteArrayAsList.newListWithStartAndEnd(source, start, end).toByteArray();
  }

  @Override
  public void decrypt(MsgBytePacket packet, ClientSession session) {
    if (packet.getBody() == null || packet.getBody().length == 0) {
      return;
    }
    try {
      byte[] data = LiveAESV3.decode(packet.getBody(), session.getUpAesKey());
      packet.setBody(data);
    } catch (Exception e) {
      throw new DecryptException("decrypt failed, msgType:" + packet.getHeader().getMsgType(), e);
    }
  }


  @Override
  public MsgBytePacket decryptToNewPacket(MsgBytePacket packet, String encryptKey) {
    if (packet.getBody() == null || packet.getBody().length == 0) {
      return null;
    }
    MsgBytePacket decryptPacket = null;
    try {
      byte[] data = LiveAESV3.decode(packet.getBody(), encryptKey);
      if (data != null){
        decryptPacket = new MsgBytePacket(packet.getHeader(), data);
      }
    } catch (Exception e) {
      throw new DecryptException("decrypt failed, msgType:" + packet.getHeader().getMsgType(), e);
    }
    return decryptPacket;
  }

}
