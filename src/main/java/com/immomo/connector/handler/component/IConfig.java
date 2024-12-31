package com.immomo.connector.handler.component;

import com.immomo.configcenter2.client.ConfigListener;
import com.immomo.configcenter2.client.exception.NotSyncedException;
import com.immomo.configcenter2.client.valuetype.Valuetype;
import java.util.Map;

public interface IConfig {

  String getConfig(String path);

  <T> T getConfig(String path, Valuetype<T> valuetype);

  <T> T getConfigWithDefaultValue(String path, Valuetype<T> valuetype, T defaultValue);

  Map<String, String> getChildren(String fullPath);

  String getConfigWithDefaultValue(String fullPath, String defaultValue);

  String getConfigWithDefaultPath(String fullPath);

  void addNodeListener(String path, ConfigListener listener) throws NotSyncedException;

  <T> void addNodeListener(String path, Valuetype<T> valuetype, ConfigListener listener) throws NotSyncedException;

  void addPathListener(String fullPath, ConfigListener listener) throws NotSyncedException;
}
