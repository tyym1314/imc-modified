package com.immomo.connector.handler.component;

import com.immomo.configcenter2.client.ConfigCenter;
import com.immomo.configcenter2.client.ConfigListener;
import com.immomo.configcenter2.client.GeneralConfig;
import com.immomo.configcenter2.client.exception.NotSyncedException;
import com.immomo.configcenter2.client.valuetype.Valuetype;
import java.util.Map;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class ConfigDelegate implements IConfig, InitializingBean {

  @Override
  public String getConfig(String path) {
    return GeneralConfig.getConfig(path);
  }

  @Override
  public <T> T getConfig(String path, Valuetype<T> valuetype) {
    return GeneralConfig.getConfig(path, valuetype);
  }

  @Override
  public <T> T getConfigWithDefaultValue(String path, Valuetype<T> valuetype, T defaultValue) {
    return GeneralConfig.getConfigWithDefaultValue(path, valuetype, defaultValue);
  }

  @Override
  public Map<String, String> getChildren(String fullPath) {
    return GeneralConfig.getChildren(fullPath);
  }

  @Override
  public String getConfigWithDefaultValue(String fullPath, String defaultValue) {
    return GeneralConfig.getConfigWithDefaultValue(fullPath, defaultValue);
  }

  @Override
  public String getConfigWithDefaultPath(String fullPath) {
    return GeneralConfig.getConfigWithDefaultPath(fullPath);
  }

  @Override
  public void addNodeListener(String path, ConfigListener listener) throws NotSyncedException {
    GeneralConfig.addNodeListener(path, listener);
  }

  @Override
  public <T> void addNodeListener(String path, Valuetype<T> valuetype, ConfigListener listener)
      throws NotSyncedException {
    GeneralConfig.addNodeListener(path, valuetype, listener);
  }

  @Override
  public void addPathListener(String fullPath, ConfigListener listener) throws NotSyncedException {
    GeneralConfig.addPathListener(fullPath, listener);
  }

  @Override
  public void afterPropertiesSet() throws Exception {
    ConfigCenter.init();
  }
}
