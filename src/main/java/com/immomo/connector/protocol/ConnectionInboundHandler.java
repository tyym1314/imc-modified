package com.immomo.connector.protocol;

import com.immomo.connector.constants.Constants;
import com.immomo.connector.handler.DecryptHandler;
import com.immomo.connector.handler.MessageDispatcher;
import com.immomo.connector.handler.UnCompressHandler;
import com.immomo.connector.monitor.HubbleUtils;
import com.immomo.connector.protocol.v1.MsgBytePacket;
import com.immomo.connector.session.Client;
import com.immomo.connector.session.ClientSession;
import com.immomo.connector.session.Const;
import com.immomo.connector.session.SessionManager;
import com.immomo.connector.tunnel.Tunnel;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelOption;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.timeout.IdleStateEvent;

import java.io.IOException;
import java.nio.channels.ClosedChannelException;
import java.util.Objects;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.logging.log4j.util.Strings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@ChannelHandler.Sharable
@Slf4j
@Service
public class ConnectionInboundHandler extends SimpleChannelInboundHandler<MsgBytePacket> {

  @Autowired
  private SessionManager sessionManager;
  @Autowired
  private DecryptHandler decryptHandler;
  @Autowired
  private UnCompressHandler unCompressHandler;
  @Autowired
  private MessageDispatcher messageDispatcher;
  @Resource
  private Tunnel tunnel;

  private static final int MAX_LENGTH = 45;
  private static final String EXCEPTION_TYPE_PREFIX = "et-";
  private static final String EXCEPTION_MESSAGE_PREFIX = "em-";

  @Override
  public void channelActive(ChannelHandlerContext ctx) throws Exception {
    HubbleUtils.incrCount("tcp-active", 1);
  }

  @Override
  public void channelWritabilityChanged(ChannelHandlerContext ctx) throws Exception {
    ClientSession session = SessionManager.get(ctx.channel());
    log.warn("writability changed, session:{}, writable:{}", session.logInfo(),
        ctx.channel().isWritable());
    HubbleUtils.incrCount("tcp-writability-changed", 1);
  }

  @Override
  public void channelInactive(ChannelHandlerContext ctx) throws Exception {
    ClientSession session = SessionManager.get(ctx.channel());
    if (session != null && session.isAuthed()) {
      log.info("channelInactive, session:{}, writable:{}", session.logInfo(),
          ctx.channel().isWritable());
      sessionManager.unRegister(session);
      HubbleUtils.incrCount("session-channel-inactive", 1);
      tunnel.close(session);
      HubbleUtils.incrCount("tcp-inactive", 1);
    }
  }

  @Override
  public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
    ClientSession session = SessionManager.get(ctx.channel());
    //fix npe
    if (session == null) {
      forceClose(ctx.channel());
      return;
    }
    HubbleUtils.incrCount("tcp-userEventTriggered", 1);
    if (evt instanceof IdleStateEvent) {
      IdleStateEvent event = (IdleStateEvent) evt;
      log.warn("session idle, state:{}, session:{}", event.state(), session.logInfo());
      session.close(Constants.CloseReason_Idle);
      HubbleUtils.incrCount("tcp-idle", 1);
    } else {
      log.warn("unknownuserevent:{}, session:{}", evt, session.logInfo());
      HubbleUtils.incrCount("tcp-idle-unknow", 1);
    }
  }

  private void forceClose(Channel channel) {
    if (!channel.isActive()) {
      return;
    }
    //关闭连上的空连接
    channel.config().setOption(ChannelOption.SO_LINGER, 0);
    channel.close();
  }

  @Override
  public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
    ClientSession session = ctx.channel().attr(Const.CLIENTSESSION).get();
    Client client =  Objects.isNull(session) ? null : session.getClient();

    String name = cause.getClass().getSimpleName(); // 使用简单名称而不是全限定名
    String subName = StringUtils.substring(name, 0, MAX_LENGTH);
    HubbleUtils.incrCountByClient(EXCEPTION_TYPE_PREFIX + subName, client);

    String message =  ExceptionUtils.getMessage(cause);
    if (StringUtils.isNotEmpty(message)) {
      //String subMsg = StringUtils.abbreviate(message, MAX_LENGTH);
      String nameMsg = StringUtils.substring(subName + "-" + message, 0, MAX_LENGTH);
      HubbleUtils.incrCountByClient(EXCEPTION_MESSAGE_PREFIX + nameMsg, client);
    }

    //解决ClosedChannelException异常重复关闭问题
    if (cause instanceof ClosedChannelException && !ctx.channel().isActive()) {
      log.error("channel is already closed, channel:{}", ctx.channel());
      HubbleUtils.incrCountByClient("tcp-closedChannelException",client);
      return;
    }

    if (session != null) {
      session.close(Constants.CloseReason_Exception);
    } else {
      HubbleUtils.incrCountByClient("tcp-nullsession",client);
//      ctx.channel().close();
      forceClose(ctx.channel());
    }

    HubbleUtils.incrCountByClient("tcp-exception",client);

    String sessionInfo = Objects.isNull(session) ? "session is null" : session.toString();
    log.error("exceptionCaught, msg:{}, session:{}", message, sessionInfo,
        cause);
  }

  @Override
  protected void channelRead0(ChannelHandlerContext ctx, MsgBytePacket packet) throws Exception {
    HubbleUtils.incrCount("client_msg_request", 1);

    if (!ctx.channel().isActive()) {
      return;
    }
    HubbleUtils.incrCount("client_msg_active_request", 1);
    ClientSession clientSession = getOrCreateSession(ctx);
    //解密
    decryptHandler.handle(packet, clientSession);
    HubbleUtils.incrCount("client_msg_decrypt_succ", 1);
    //解压缩
    unCompressHandler.handle(packet, clientSession);
    HubbleUtils.incrCount("client_msg_uncompress_succ", 1);
    //消息分发
    messageDispatcher.handle(packet, clientSession);
    HubbleUtils.incrCount("client_msg_dispatcher_succ", 1);

  }

  private ClientSession getOrCreateSession(ChannelHandlerContext ctx) {
    ClientSession clientSession = SessionManager.get(ctx.channel());
    if (clientSession == null) {
      clientSession = new ClientSession(ctx.channel());
      ctx.channel().attr(Const.CLIENTSESSION).set(clientSession);
    }
    return clientSession;
  }


}
