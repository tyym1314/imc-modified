package com.immomo.connector.tunnel;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Sets;
import com.immomo.connector.util.http.OkHttpUtils;
import com.immomo.mcf.util.JsonUtils;
import com.immomo.mcf.util.StringUtils;
import java.util.Map;
import java.util.Set;
import javax.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * @author yang.zhaocheng@immomo.com
 * @description
 * @data 2022/03/21 下午4:34
 **/
@Component
@Slf4j
public class ImProxyTunnelService {

  @Resource
  private OkHttpUtils okHttpUtils;

  private final String PRE_PROXY_URL = "https://a.b.com/execute";
  private final String PROXY_URL = "https://b.c.com/execute";
  private final String ACTION = "/service/live-im-proxy";

  public Set<String> getTunnelMomoids(){
    String res = getMethod(ACTION, "getTunnelMomoids", new String[0]);
    if (res != null){
      return Sets.newHashSet(JsonUtils.toTList(res, String.class));
    }
    return Sets.newHashSet();
  }

  public Boolean existTunnelMomoid(String momoid){
    String res = getMethod(ACTION, "existTunnelMomoid", new String[]{momoid});
    return JsonUtils.toT(res, Boolean.class);
  }

  public Boolean delTunnelMomoid(String momoid){
    String res = getMethod(ACTION, "delTunnelMomoid", new String[]{momoid});
    return JsonUtils.toT(res, Boolean.class);
  }


  public Boolean pushImData(String momoid, String dataStr){
    String res = getMethod(ACTION, "pushImData", new String[]{momoid, dataStr});
    return JsonUtils.toT(res, Boolean.class);
  }


  public String getMethod(String action, String method, String[] params) {
    try {
      MoaData moaData = new MoaData(action, method, params);
      String res = okHttpUtils.doPostByForm(PROXY_URL, null, ImmutableMap.of("data", JsonUtils.toJSON(moaData)), 500);
      //log.info(String.format("getMethod action %s,method:%s, params %s,res %s ", action, method, JsonUtils.toJSON(params), res));
      if (res != null){
        Map<String, Object> resMap = JsonUtils.toMap(res);
        if (((Integer)resMap.getOrDefault("ec", 1) == 0) &&
            StringUtils.equalsIgnoreCase((String)resMap.getOrDefault("em", ""), "OK")){
           return JsonUtils.toJSON(resMap.get("result"));
        }
      }
      return res;
    } catch (Exception e) {
      log.error(String.format("getMethod action %s,method:%s, params %s error ", action, method, JsonUtils.toJSON(params)), e);
    }
    return null;
  }


  public static class MoaData{
    private String action;
    private String method;
    private String[] params;

    public MoaData(){

    }

    public MoaData(String action, String method, String[] params){
      this.action = action;
      this.method = method;
      this.params = params;
    }

    public String getAction() {
      return action;
    }

    public void setAction(String action) {
      this.action = action;
    }

    public String getMethod() {
      return method;
    }

    public void setMethod(String method) {
      this.method = method;
    }

    public String[] getParams() {
      return params;
    }

    public void setParams(String[] params) {
      this.params = params;
    }
  }
}
