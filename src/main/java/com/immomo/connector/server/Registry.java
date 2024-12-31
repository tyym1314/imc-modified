package com.immomo.connector.server;

import com.google.common.base.Charsets;
import com.google.common.util.concurrent.ThreadFactoryBuilder;
import com.immomo.connector.config.Config;
import com.immomo.connector.util.ImAddrUtil;
import com.immomo.connector.util.ImIpUtils;
import com.immomo.env.MomoEnv;
import com.immomo.mcf.util.JsonUtils;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.retry.BoundedExponentialBackoffRetry;
import org.apache.curator.utils.CloseableUtils;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class Registry implements ConnectionStateListener, InitializingBean,
    DisposableBean {

  private final ConcurrentMap<String, Set<ServerNode>> serverNodes = new ConcurrentHashMap<>();

  @Autowired
  private Config config;

  private CuratorFramework client;
  private volatile boolean stopped;

  private ScheduledExecutorService keeprThread;

  public void start() {
    try {
      client = CuratorFrameworkFactory.newClient(config.getZkAddress(), 8000, 4000,
          new BoundedExponentialBackoffRetry(1000, 60 * 60 * 1000, 2000));
      client.getConnectionStateListenable().addListener(this);
      client.start();
      client.blockUntilConnected();

      keeprThread = Executors.newSingleThreadScheduledExecutor(
          new ThreadFactoryBuilder().setNameFormat("RegistryKeeper").build());
      keeprThread.scheduleWithFixedDelay(this::keep, 2, 2, TimeUnit.MINUTES);
      log.info("registry started");
    } catch (Exception e) {
      log.error("error start registry", e);
    }
  }

  private void keep() {
    if (stopped) {
      log.error("registry stopped");
      return;
    }

    serverNodes.forEach((protocol, nodes) -> {
      nodes.forEach(node -> {
        try {
          String path = ZKPaths.makePath(protocol, node.name());
          if (client.checkExists().forPath(path) == null) {
            write(protocol, node);
          }
        } catch (Exception e) {
          log.error("error keep node, protocol:{}, node:{}", protocol, node, e);
          //NOOP
        }
        log.info("keep node, protocol:{}, node:{}", protocol, node);
      });
    });
  }

  public void stop() {
    serverNodes.forEach((path, nodes) -> {
      nodes.forEach(node -> delete(path, node));
    });
    CloseableUtils.closeQuietly(client);
  }

  private void write(String basePath, ServerNode serverNode) {
    String path = ZKPaths.makePath(basePath, serverNode.name());
    String data = JsonUtils.toJSON(serverNode);
    try {
      client.create().creatingParentsIfNeeded()
          .withMode(CreateMode.EPHEMERAL)
          .forPath(path, data.getBytes(Charsets.UTF_8));
      log.info("data written to zk, path:{}, data:{}", path, data);
    } catch (NodeExistsException e) {
      try {
        delete(basePath, serverNode);
        client.create().creatingParentsIfNeeded()
            .withMode(CreateMode.EPHEMERAL)
            .forPath(path, data.getBytes(Charsets.UTF_8));
        log.info("data written to zk, path:{}, data:{}", path, data);
      } catch (Exception e1) {
        e1.printStackTrace();
        log.error("error write server node to zk, path:{}", path, e1);
      }
    } catch (Exception e) {
      e.printStackTrace();
      log.error("error write server node to zk, path:{}", path, e);
    }
  }

  private void delete(String basePath, ServerNode serverNode) {
    String path = ZKPaths.makePath(basePath, serverNode.name());
    try {
      client.delete().forPath(path);
      log.info("remove node from cluster, path:{}", path);
    } catch (Exception e) {
      log.error("remove node error, path:{}", path, e);
    }
  }

  public void register(String protocolPath, int port) {
    if (stopped) {
      return;
    }

    Set<ServerNode> nodes = serverNodes.get(protocolPath);
    if (nodes == null) {
      nodes = new HashSet<>();
      Set<ServerNode> existed = serverNodes.putIfAbsent(protocolPath, nodes);
      if (existed != null) {
        nodes = existed;
      }
    }

    ServerNode node = new ServerNode(true, port);
    if (!nodes.add(node)) {
      log.warn("node exists, path:{}, node:{}", protocolPath, node.name());
      return;
    }

    write(protocolPath, node);
  }

  public void unRegister(String protocolPath, int port) {
    if (stopped) {
      return;
    }

    Set<ServerNode> nodes = serverNodes.get(protocolPath);
    if (nodes == null) {
      return;
    }
    ServerNode node = new ServerNode(true, port);
    if (!nodes.remove(node)) {
      return;
    }

    delete(protocolPath, node);
  }

  @Override
  public void stateChanged(CuratorFramework client, ConnectionState newState) {
    if (!newState.isConnected()) {
      return;
    }
    log.info("state changed, re-register all nodes");
    serverNodes.forEach((path, nodes) -> {
      nodes.forEach(node -> {
        delete(path, node);
        write(path, node);
      });
    });
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    start();
  }

  @Override
  public void destroy() throws Exception {
    stopped = true;
    keeprThread.shutdown();
    stop();
    log.info("registry stopped");
  }

  private static class ServerNode {

    private String ip_inner;
    private boolean auto_connect;
    private boolean aliYun;
    private String aliYunEcsInstanceId;
    private String timestamp;
    private int port;
    private String ip_outer;

    public ServerNode(boolean auto_connect, int port) {
      this.timestamp = System.currentTimeMillis() + "";
      this.ip_inner = ImIpUtils.IP_LAN;
      this.port = port;
      this.auto_connect = auto_connect;
      this.aliYun = ImAddrUtil.isYunMachine();
      this.ip_outer = ImAddrUtil.getOuterIPv4();
    }

    public String name() {
      return ip_inner + ":" + port;
    }

    public String getIp_inner() {
      return ip_inner;
    }

    public void setIp_inner(String ip_inner) {
      this.ip_inner = ip_inner;
    }

    public boolean isAuto_connect() {
      return auto_connect;
    }

    public void setAuto_connect(boolean auto_connect) {
      this.auto_connect = auto_connect;
    }

    public boolean isAliYun() {
      return aliYun;
    }

    public void setAliYun(boolean aliYun) {
      this.aliYun = aliYun;
    }

    public String getAliYunEcsInstanceId() {
      return aliYunEcsInstanceId;
    }

    public void setAliYunEcsInstanceId(String aliYunEcsInstanceId) {
      this.aliYunEcsInstanceId = aliYunEcsInstanceId;
    }

    public String getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(String timestamp) {
      this.timestamp = timestamp;
    }

    public int getPort() {
      return port;
    }

    public void setPort(int port) {
      this.port = port;
    }

    public String getIp_outer() {
      return ip_outer;
    }

    public void setIp_outer(String ip_outer) {
      this.ip_outer = ip_outer;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ServerNode)) {
        return false;
      }
      ServerNode that = (ServerNode) o;
      return getPort() == that.getPort() &&
          Objects.equals(getIp_inner(), that.getIp_inner());
    }

    @Override
    public int hashCode() {
      return Objects.hash(getIp_inner(), getPort());
    }
  }
}
