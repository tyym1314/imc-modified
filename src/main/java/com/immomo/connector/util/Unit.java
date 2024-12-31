package com.immomo.connector.util;

import com.immomo.mcf.util.JsonUtils;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.collections.MapUtils;

public class Unit implements Serializable {
  private static final long serialVersionUID = -1127369371232267383L;

  private String t;

  private Map<String, Object> p;

  /**
   * new Unit 的时间
   */
  private long tt;

  public Unit() {
    this("");
  }


  public Unit(String t) {
    this.p = new HashMap<String, Object>();
    this.t = t;
    this.tt = System.currentTimeMillis();
  }

  public boolean hasParam(String key) {
    return this.p.containsKey(key);
  }

  public <T> T getParam(String k, Class<T> t) {
    Object obj = this.p.get(k);
    return obj == null ? null : (T) obj;
  }

  public int getIntParam(String key) {
    return MapUtils.getIntValue(p, key);
  }

  public String getStringParam(String key) {
    return MapUtils.getString(p, key);
  }

  public Long getLongParam(String key) {
    return MapUtils.getLong(p, key);
  }

  public Boolean getBooleanParam(String key) {
    return MapUtils.getBoolean(p, key);
  }

  public String toJson() {
    return JsonUtils.toJSON(this);
  }

  public static Unit parse(String json) {
    return JsonUtils.toT(json, Unit.class);
  }


  @Override
  public String toString() {
    return "Unit{" + "t='" + t + '\'' + ", p=" + p + ", tt=" + tt + '}';
  }

  public String getT() {
    return t;
  }

  public void setT(String t) {
    this.t = t;
  }

  public Map<String, Object> getP() {
    return p;
  }

  public void setP(Map<String, Object> p) {
    this.p = p;
  }

  public long getTt() {
    return tt;
  }

  public void setTt(long tt) {
    this.tt = tt;
  }

  public Unit setParam(String k, Object v) {
    this.p.put(k, v);
    return this;
  }
}
