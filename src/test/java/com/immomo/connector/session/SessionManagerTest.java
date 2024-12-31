package com.immomo.connector.session;

import com.google.protobuf.InvalidProtocolBufferException;
import com.immomo.live.im.base.bean.KickMsg;
import com.immomo.mcf.util.JsonUtils;
import java.util.Base64;
import org.junit.Ignore;
import org.junit.Test;
import room.Live.Im;

/**
 * Created By wlb on 2019-09-26 12:19
 */
public class SessionManagerTest {

  @Ignore
  @Test
  public void testCacheGC() {
    SessionManager manager = new SessionManager();

  }


  @Ignore
  @Test
  public void testPb() throws InvalidProtocolBufferException {
    String base = "Cgg2QTQyNUI0NRChtLPO7i0aSBIRMTU3NTg4MDU0Njg0OS45MDgaCDcyMTU2OTc0IgYyMTQzMjkyEDIxMDAxMjQyMDY1NzAwMjM6DDEwLjM3LjMzLjE5OUCaCA==";
    byte[] bytes = Base64.getDecoder().decode(base);
    Im im = Im.parseFrom(bytes);
    System.out.println(im.getClient());
  }


  @Test
  public void testKick(){
    KickMsg kickMsg = new KickMsg("384689CE1750D4FE11F9DC40E42C410A", "169504", "127.0.0.1:2181");
    System.out.println(JsonUtils.toJSON(kickMsg));
  }
}