package com.immomo.connector.util.http;

import com.google.common.base.Stopwatch;
import com.immomo.connector.monitor.HubbleUtils;
import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.TimeUnit;
import okhttp3.FormBody;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Request.Builder;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;


@Component
public class OkHttpUtils {

  private static Logger logger = LoggerFactory.getLogger(OkHttpUtils.class);
  private static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
  private static final MediaType FORM = MediaType.parse("application/x-www-form-urlencoded");


  private final OkHttpClient client = new OkHttpClient.Builder().connectTimeout(3000, TimeUnit.MILLISECONDS).readTimeout(100, TimeUnit.MILLISECONDS)
      .build();

  private String doGet(String url, Map<String, String> headers, OkHttpClient client) throws IOException {
    Stopwatch stop = Stopwatch.createStarted();
    String result = null;
    Request request;
    try {
      Builder builder = new Builder().url(url);
      if (headers != null && !headers.isEmpty()) {
        for (Entry<String, String> entry : headers.entrySet()) {
          builder.addHeader(entry.getKey(), entry.getValue());
        }
      }
      request = builder.build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        result = response.body().string();
      } else {
        throw new IOException("request failed " + response);
      }
    } catch (Exception e) {
      HubbleUtils.incrCount("http.timeout");
      logger.error(String.format("OkHttpUtil.doGet error,url:%s", url), e);
      throw e;
    }
    HubbleUtils.incrCount("http.req");
    logger.info("OkHttpUtil.doGet url:{},headers:{},result:{},cost:{}ms", url, headers, result, stop.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  public String doGet(String url, Map<String, String> headers) throws IOException {
    return doGet(url, headers, client);
  }

  public String doGet(String url, Map<String, String> headers, int timeout) throws IOException {
    return doGet(url, headers, client.newBuilder().readTimeout(timeout, TimeUnit.MILLISECONDS).build());
  }

  public boolean isSuccess(String url) {
    try {
      return client.newCall(new Builder().url(url).build()).execute().isSuccessful();
    } catch (IOException e) {
      //超时也没关系，主要是检测连接通不通
      return true;
    }
  }

  private String doPost(String url, Map<String, String> headers, String json, OkHttpClient client) throws IOException {
    Stopwatch stop = Stopwatch.createStarted();
    String result = null;
    Request request;
    try {
      Builder builder = new Builder().url(url);
      if (headers != null && !headers.isEmpty()) {
        for (Entry<String, String> entry : headers.entrySet()) {
          builder.addHeader(entry.getKey(), entry.getValue());
        }
      }
      RequestBody body = RequestBody.create(JSON, json);
      request = builder.post(body).build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        result = response.body().string();
      } else {
        throw new IOException("request failed " + response);
      }
    } catch (Exception e) {
      HubbleUtils.incrCount("http.timeout");
      logger.error(String.format("OkHttpUtil.doPost error,url:%s", url), e);
      throw e;
    }
    HubbleUtils.incrCount("http.req");
    //logger.info("OkHttpUtil.doPost url:{},headers:{},result:{},cost:{}ms", url, headers, result, stop.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  public String doPostByForm(String url, Map<String, String> headers, Map<String,String> dataMap, int timeout) throws IOException {
    return doPostByForm(url, headers, dataMap, client.newBuilder().readTimeout(timeout, TimeUnit.MILLISECONDS).connectTimeout(500, TimeUnit.MILLISECONDS).build());
  }

  private String doPostByForm(String url, Map<String, String> headers, Map<String,String> dataMap, OkHttpClient client) throws IOException {
    Stopwatch stop = Stopwatch.createStarted();
    String result = null;
    Request request;
    try {
      //header
      Builder builder = new Builder().url(url);
      if (headers != null && !headers.isEmpty()) {
        for (Entry<String, String> entry : headers.entrySet()) {
          builder.addHeader(entry.getKey(), entry.getValue());
        }
      }

      //body
      FormBody.Builder bodyBuilder = new FormBody.Builder();
      if (dataMap != null && !dataMap.isEmpty()) {
        for (Entry<String, String> entry : dataMap.entrySet()) {
          bodyBuilder.add(entry.getKey(), entry.getValue());
        }
      }

      request = builder
          .url(url)
          .post(bodyBuilder.build())
          .build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        result = response.body().string();
      } else {
        throw new IOException("request failed " + response);
      }
    } catch (Exception e) {
      HubbleUtils.incrCount("http.timeout");
      logger.error(String.format("OkHttpUtil.doPost error,url:%s", url), e);
      throw e;
    }
    HubbleUtils.incrCount("http.req");
    //logger.info("OkHttpUtil.doPost url:{},headers:{},result:{},cost:{}ms", url, headers, result, stop.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  public String doPost(String url, Map<String, String> headers, String json, int timeout) throws IOException {
    return doPost(url, headers, json, client.newBuilder().readTimeout(timeout, TimeUnit.MILLISECONDS).build());
  }

  private String doPut(String url, Map<String, String> headers, String json, OkHttpClient client) {
    Stopwatch stop = Stopwatch.createStarted();
    String result = null;
    Request request;
    try {
      Builder builder = new Builder().url(url);
      if (headers != null && !headers.isEmpty()) {
        for (Entry<String, String> entry : headers.entrySet()) {
          builder.addHeader(entry.getKey(), entry.getValue());
        }
      }
      RequestBody body = RequestBody.create(JSON, json);
      request = builder.put(body).build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        result = response.body().string();
      } else {
        throw new IOException("request failed " + response);
      }
    } catch (Exception e) {
      HubbleUtils.incrCount("http.timeout");
      logger.error(String.format("OkHttpUtil.doPut error,url:%s", url), e);
    }
    HubbleUtils.incrCount("http.req");
    logger.info("OkHttpUtil.doPut url:{},headers:{},result:{},cost:{}ms", url, headers, result, stop.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  public String doPut(String url, Map<String, String> headers, String json, int timeout) {
    return doPut(url, headers, json, client.newBuilder().readTimeout(timeout, TimeUnit.MILLISECONDS).build());
  }

  private String doDelete(String url, Map<String, String> headers, String json, OkHttpClient client) {
    Stopwatch stop = Stopwatch.createStarted();
    String result = null;
    Request request;
    try {
      Builder builder = new Builder().url(url);
      if (headers != null && !headers.isEmpty()) {
        for (Entry<String, String> entry : headers.entrySet()) {
          builder.addHeader(entry.getKey(), entry.getValue());
        }
      }
      RequestBody body = RequestBody.create(JSON, json);
      request = builder.delete(body).build();
      Response response = client.newCall(request).execute();
      if (response.isSuccessful()) {
        result = response.body().string();
      } else {
        throw new IOException("request failed " + response);
      }
    } catch (Exception e) {
      HubbleUtils.incrCount("http.timeout");
      logger.error(String.format("OkHttpUtil.doDelete error,url:%s", url), e);
    }
    HubbleUtils.incrCount("http.req");
    logger.info("OkHttpUtil.doDelete url:{},headers:{},result:{},cost:{}ms", url, headers, result, stop.elapsed(TimeUnit.MILLISECONDS));
    return result;
  }

  public String doDelete(String url, Map<String, String> headers, String json, int timeout) {
    return doDelete(url, headers, json, client.newBuilder().readTimeout(timeout, TimeUnit.MILLISECONDS).build());
  }
}
