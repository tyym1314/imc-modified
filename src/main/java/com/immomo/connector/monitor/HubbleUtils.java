package com.immomo.connector.monitor;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.immomo.connector.session.Client;
import com.immomo.hubble.client.HubbleClient;
import com.immomo.hubble.client.HubbleClientFactory;
import com.immomo.hubble.client.common.MonitorSource;
import com.immomo.hubble.client.monitor.BaseMonitor;
import com.immomo.hubble.client.monitor.BasicStatMonitor;
import com.immomo.hubble.client.monitor.CounterMonitor;
import com.immomo.hubble.client.monitor.DistributionSummaryMonitor;

import java.util.Objects;
import java.util.concurrent.Callable;

/**
 * Created By wlb on 2019-11-28 12:11
 */
public class HubbleUtils {

  private static final HubbleClient client = HubbleClientFactory.getHubbleClientBySource(
      MonitorSource.BUSINESS);

  private static final Cache<String, BaseMonitor> monitors = CacheBuilder.newBuilder()
      .maximumSize(2048).build();

  private static final String ACTION = "im-connector";

  private static <T extends BaseMonitor> T get(String key, Callable<T> callable) {
    try {
      return (T) monitors.get(key, callable);
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static void addTime(String key, long time) {
    DistributionSummaryMonitor summaryMonitor = get(key,
        () -> client.newDistributionSummary(ACTION, key, new int[]{100, 200, 500, 1000, 2000}));
    summaryMonitor.update(time);
  }

  public static void incrCount(String key, int value) {
    CounterMonitor counterMonitor = get(key, () -> client.newCounter(ACTION, key));
    counterMonitor.incr();
  }

  public static void incrCount(String key) {
    CounterMonitor counterMonitor = get(key, () -> client.newCounter(ACTION, key));
    counterMonitor.incr();
  }

  public static void incrCountByClient(String key, Client clientType) {
    incrCount(key);
    if (Objects.isNull(clientType)) {
      return;
    }
    String keyByClient = key + "-" + clientType.name();
    CounterMonitor counterMonitor = get(keyByClient, () -> client.newCounter(ACTION, keyByClient));
    counterMonitor.incr();
  }

  public static void addBasicStat(String key, double value) {
    BasicStatMonitor basicStatMonitor = get(key, () -> client.newBasicStat(ACTION, key));
    basicStatMonitor.add(value);
  }

  public static void outBoundBufferMonitor(String key, long bytes) {
    DistributionSummaryMonitor summaryMonitor = get(key,
            () -> client.newDistributionSummary(ACTION, key, new int[]{10000, 100000, 500000, 1000000, 2000000}));
    summaryMonitor.update(bytes);
  }

}
