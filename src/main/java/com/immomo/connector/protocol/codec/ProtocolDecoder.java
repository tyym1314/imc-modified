package com.immomo.connector.protocol.codec;

import com.immomo.connector.protocol.MsgBytePackets;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageDecoder;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ProtocolDecoder extends ByteToMessageDecoder {
  @Override
  protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
    if (in.readableBytes() > 0) {
      out.add(MsgBytePackets.unPacket(in));
    }
  }
}
