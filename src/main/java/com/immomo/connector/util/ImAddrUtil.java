package com.immomo.connector.util;

import com.immomo.env.MomoEnv;
import java.util.concurrent.TimeUnit;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;

/**
 * Created By wlb on 2019-11-27 17:49
 */
@Slf4j
public class ImAddrUtil {

  private static final OkHttpClient client = new OkHttpClient.Builder()
      .connectTimeout(5, TimeUnit.SECONDS)
      .writeTimeout(5, TimeUnit.SECONDS)
      .readTimeout(10, TimeUnit.SECONDS)
      .build();

  public static String getOuterIPv4() {
    String url;
    String zone = MomoEnv.zone();
    String corp = MomoEnv.corp();
    if (corp.equals("overseas") || corp.equals("aws-us-east-1")) {
      //aws
      if (zone.equals("sg-aws") || zone.equals("aws-us-east-1a")) {
        url = "http://1.1.1.1/latest/meta-data/public-ipv4";
      } else {
        //aliyun
        url = "http://1.1.1.1/latest/meta-data/eipv4";
      }
    }  else {
      if (corp.equals("alpha")) {
        return "1.1.1.1";
      }
      throw new IllegalStateException("not supported zone: " + zone + " corp: " + corp);
    }

    Request request = new Request.Builder().url(url).build();
    try (Response response = client.newCall(request).execute()) {
      return response.body().string();
    } catch (Exception e) {
      throw new IllegalStateException(e);
    }
  }

  public static boolean isYunMachine() {
    return true;
  }
}
