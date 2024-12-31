package com.immomo.connector.protocol.codec;

import com.immomo.connector.protocol.MsgBytePackets;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.tunnel.Tunnel;
import com.immomo.connector.util.SpringBeanUtils;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Sharable
public class ProtocolEncoder extends MessageToByteEncoder {

  @Override
  protected void encode(ChannelHandlerContext channelHandlerContext, Object msg, ByteBuf out)
      throws Exception {
    if (msg instanceof MsgBytePacket) {
      MsgBytePackets.packet((MsgBytePacket) msg, out);
      Tunnel tunnel =  SpringBeanUtils.getBean(Tunnel.class);
      if (tunnel != null){
        tunnel.recordTunnel(channelHandlerContext, (MsgBytePacket) msg);
      }else {
        log.error("not find tunnel bean");
      }
    }
  }


}
