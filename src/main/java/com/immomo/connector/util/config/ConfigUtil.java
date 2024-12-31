package com.immomo.connector.util.config;

import com.immomo.configcenter2.client.ConfigCenter;
import com.immomo.configcenter2.client.GeneralConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.math.NumberUtils;



/**
 * @Author hehu
 * @Date 2024/3/15 16:13
 **/
@Slf4j
public class ConfigUtil {



    static {
        try {
            ConfigCenter.init();
        } catch (Exception e) {
            log.error("configcenter init fail!!!!!!", e);
        }
    }


    public static String getValue(String configKey) {
        if (StringUtils.isBlank(configKey)) {
            return StringUtils.EMPTY;
        }
        String configValue = GeneralConfig.getConfigWithDefaultValue(configKey, StringUtils.EMPTY);
        if (StringUtils.isBlank(configValue)) {
            return StringUtils.EMPTY;
        }
        return configValue;
    }

    public static long overOutboundBufferLimit() {
        String outboundbufferLimit = getValue("outboundbuffer_limit");
        return NumberUtils.toLong(outboundbufferLimit, 100000);
    }

    public static int splitSendBroadcastTimes() {
        String outboundbufferLimit = getValue("split_broadcast_times");
        return NumberUtils.toInt(outboundbufferLimit, 10);
    }

    public static boolean canDiscardMsg() {
        String discardMsg = getValue("can_discard_msg");
        return NumberUtils.toInt(discardMsg, 0) == 1;
    }
    



}
