package com.immomo.connector.dao.impl;

import com.immomo.connector.config.Config;
import com.immomo.connector.dao.IUserDao;
import com.immomo.momostore.proxy.IStoreDao;
import com.immomo.momostore.proxy.StoreDaoFactory;
import com.immomo.msc.cache.CacheLoader;
import com.immomo.msc.cache.MscCache;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

@Repository
@Slf4j
public class UserDao implements IUserDao, InitializingBean {

  @Autowired
  private Config config;

  private IStoreDao storeDao;


  private static final ConcurrentMap<String, MscCache<String, Optional<String>>> concurrentMap = new ConcurrentHashMap<>();

  private MscCache<String, Optional<String>> cache(String appId) {

    MscCache<String, Optional<String>> cache = concurrentMap.get(appId);
    if (cache == null) {
      cache = MscCache.newBuilder("im-connector-user-country-" + appId)
          .maximumSize(10000)
          .expireAfterWrite(5, TimeUnit.MINUTES)
          .build(new CacheLoader<String, Optional<String>>() {
            @Override
            public Optional<String> load(String userId) throws Exception {
              return Optional.ofNullable(loadCountry(appId, userId));
            }
          });

      MscCache<String, Optional<String>> existCache = concurrentMap.putIfAbsent(appId, cache);
      if (existCache != null) {
        cache = existCache;
      }
    }

    return cache;
  }

  private String loadCountry(String appId, String userId) {
    try {
      return storeDao.get(userId, String.format("appid:%s:userid:%s:user_country", appId, userId));
    } catch (Exception e) {
      log.error("load user country error userId:{}", userId, e);
    }

    return null;
  }


  @Override
  public String getCountry(String appId, String userId) {
    try {
      return cache(appId).get(userId).orElse(null);
    } catch (ExecutionException e) {
      log.error("countryCache.get({}) error", userId, e);
    }

    return null;
  }


  @Override
  public void afterPropertiesSet() throws Exception {
    storeDao = StoreDaoFactory.createStoreDao(config.getUserCluster());
    log.info("redis user cluster:{}", config.getUserCluster());
  }
}
