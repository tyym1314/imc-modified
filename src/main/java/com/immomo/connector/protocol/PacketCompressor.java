package com.immomo.connector.protocol;

import com.immomo.connector.protocol.v1.MsgBytePacket;
import java.util.function.Supplier;

import com.immomo.env.MomoEnv;
import lombok.extern.slf4j.Slf4j;
import org.xerial.snappy.Snappy;

/**
 * Created By wlb on 2019-04-17 21:15
 */
@Slf4j
public class PacketCompressor {

  private Supplier<Integer> sizeLimiter;

  public PacketCompressor() {
    sizeLimiter = () -> 512;
  }

  public PacketCompressor(Supplier<Integer> sizeLimiter) {
    this.sizeLimiter = sizeLimiter;
  }

  public void compress(final MsgBytePacket rawPacket) {
    byte[] body = rawPacket.getBody();
    int length = (body == null) ? 0 : body.length;
    if (length < sizeLimiter.get()) {
      return;
    }

    try {
      byte[] compressed = Snappy.compress(body);
      if (MomoEnv.corp().equals("alpha")) {
        log.info("compress, size before:{}, after:{}", length, compressed.length);
      }
      if (compressed.length >= length) {
        log.error("compress size:{} >= raw size:{}", compressed.length, length);
        return;
      }
      rawPacket.getHeader().setExt((byte) 1);
      rawPacket.setBody(compressed);
    } catch (Exception e) {
      log.error("error compress", e);
    }
  }

}
