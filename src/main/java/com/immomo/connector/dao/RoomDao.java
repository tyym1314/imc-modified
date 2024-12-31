package com.immomo.connector.dao;

import com.immomo.connector.config.Config;
import com.immomo.mcf.util.JsonUtils;
import com.immomo.momostore.proxy.IStoreDao;
import com.immomo.momostore.proxy.StoreDaoFactory;
import com.immomo.msc.cache.CacheLoader;
import com.immomo.msc.cache.MscCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

/**
 * @Author hehu
 * @Date 2024/6/6 17:38
 **/
@Repository
@Slf4j
public class RoomDao implements  InitializingBean {


    private IStoreDao roomRedis;


    @Autowired
    private Config config;

    private static final ConcurrentMap<String, MscCache<String, Optional<String>>> concurrentMap = new ConcurrentHashMap<>();

    private MscCache<String, Optional<String>> cache(String appId) {

        MscCache<String, Optional<String>> cache = concurrentMap.get(appId);
        if (cache == null) {
            cache = MscCache.newBuilder("im-connector-room-info-" + appId)
                    .maximumSize(10000)
                    .expireAfterWrite(5, TimeUnit.MINUTES)
                    .build(new CacheLoader<String, Optional<String>>() {
                        @Override
                        public Optional<String> load(String roomId) throws Exception {
                            return Optional.ofNullable(loadRoomInfo(roomId));
                        }
                    });

            MscCache<String, Optional<String>> existCache = concurrentMap.putIfAbsent(appId, cache);
            if (existCache != null) {
                cache = existCache;
            }
        }

        return cache;
    }

    private String loadRoomInfo(String roomId) {
        try {
            return roomRedis.get(roomId, String.format("sc_room:%s:info", roomId));
        } catch (Exception e) {
            log.error("load room info error userId:{}", roomId, e);
        }

        return null;
    }


    public int getRoomLiveMode(String appId, String roomId) {
        try {
            String roomInfo = cache(appId).get(roomId).orElse(null);
            if (StringUtils.isNotEmpty(roomInfo)) {
                Map<String, Object> map = JsonUtils.toMap(roomInfo);
                return MapUtils.getIntValue(map, "live_mode", 0);
            }
            return 0;
        } catch (ExecutionException e) {
            log.error("countryCache.get({}) error", roomId, e);
        }
        return 0;
    }



    @Override
    public void afterPropertiesSet() throws Exception {
        roomRedis = StoreDaoFactory.createStoreDao(config.getRoomRedis());
        log.info("redis room cluster:{}", config.getRoomRedis());

    }
}
