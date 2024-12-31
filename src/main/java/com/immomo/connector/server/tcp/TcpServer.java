package com.immomo.connector.server.tcp;

import com.google.common.collect.Maps;
import com.immomo.connector.config.ProjectConfig;
import com.immomo.connector.protocol.ConnectionInboundHandler;
import com.immomo.connector.protocol.codec.ProtocolDecoder;
import com.immomo.connector.protocol.codec.ProtocolEncoder;
import com.immomo.connector.server.Registry;
import com.immomo.hubble.client.HubbleClient;
import com.immomo.hubble.client.HubbleClientFactory;
import com.immomo.hubble.client.HubbleOutputer;
import com.immomo.hubble.client.common.MonitorSource;
import com.immomo.mcf.monitor.MonitorManager;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.codec.LengthFieldPrepender;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.internal.SystemPropertyUtil;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.DependsOn;
import org.springframework.stereotype.Service;


@Service
@Slf4j
@DependsOn(value = {"kafkaProducer"})
/**
 * tcp 负责链接 必须dependsON kafkaProducer producer 先初始化 后销毁
 */
public class TcpServer implements InitializingBean, DisposableBean {

  @Autowired
  private Registry registry;
  @Autowired
  private ConnectionInboundHandler connectionInboundHandler;
  private int port;

  private final NioEventLoopGroup bossGroup = new NioEventLoopGroup();
  private final NioEventLoopGroup workerGroup = new NioEventLoopGroup();
  private final LoggingHandler loggingHandler = new LoggingHandler(LogLevel.INFO);
  private final LengthFieldPrepender lengthFieldPrepender = new LengthFieldPrepender(4);
  private final ProtocolEncoder protocolEncoder = new ProtocolEncoder();



  protected void start(int port) {
    this.port = port;

    ServerBootstrap nettyServer = new ServerBootstrap();
    nettyServer.channel(NioServerSocketChannel.class).localAddress(port)
        .group(bossGroup, workerGroup)
        .handler(loggingHandler)
        .childHandler(new ChannelInitializer<SocketChannel>() {
          @Override
          public void initChannel(SocketChannel channel) throws Exception {
            channel.pipeline().addLast(loggingHandler);
            channel.pipeline().addLast(new IdleStateHandler(2, 0, 0, TimeUnit.MINUTES));

            channel.pipeline().addLast(new LengthFieldBasedFrameDecoder(16 * 1024, 0, 4, 0, 4));
            channel.pipeline().addLast(lengthFieldPrepender);

            channel.pipeline().addLast(new ProtocolDecoder());
            channel.pipeline().addLast(protocolEncoder);

            channel.pipeline().addLast(connectionInboundHandler);
          }
        });

    nettyServer.option(ChannelOption.SO_BACKLOG, 2048);
    nettyServer.option(ChannelOption.SO_REUSEADDR, true);
    nettyServer.option(ChannelOption.SO_RCVBUF, 256 * 1024);
    nettyServer.option(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);

    nettyServer.childOption(ChannelOption.ALLOCATOR, PooledByteBufAllocator.DEFAULT);
    nettyServer.childOption(ChannelOption.SO_KEEPALIVE, true);
    nettyServer.childOption(ChannelOption.TCP_NODELAY, true);

    nettyServer.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(384 * 1024, 512 * 1024));

    nettyServer.childOption(ChannelOption.SO_RCVBUF, 256 * 1024);
    nettyServer.childOption(ChannelOption.SO_SNDBUF, 1024 * 1024);

    nettyServer.bind().addListener(f -> {
      if (f.isSuccess()) {
        registry.register(ProjectConfig.Registry.ZK_PATH, port);
        log.info("tcpServer.register success port:" + port);
      } else {
        log.info("tcpServer.register failed port:" + port);
        System.exit(-1);
      }
    });

  }

  @Override
  public void destroy() throws Exception {
    log.info("stop server start...");
    registry.unRegister(ProjectConfig.Registry.ZK_PATH, port);

    bossGroup.shutdownGracefully();
    workerGroup.shutdownGracefully();
    log.info("stop server ...");
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    int port = SystemPropertyUtil.getInt("com.immomo.im.connector.tcpPort", 55555);
    start(port);
  }
}
