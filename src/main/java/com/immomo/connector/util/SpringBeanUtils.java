package com.immomo.connector.util;

import java.util.Map;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

@Component
public class SpringBeanUtils implements ApplicationContextAware {

  private static ApplicationContext APPLICATION_CONTEXT;

  @Override
  public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
    APPLICATION_CONTEXT = applicationContext;
  }

  public static <T> T getBean(Class<T> clazz) {
    return APPLICATION_CONTEXT.getBean(clazz);
  }

  public static<T> Map<String, T> getBeansOfType(Class<T> clazz) {
    return APPLICATION_CONTEXT.getBeansOfType(clazz);
  }

  public static Object getBean(String name) throws BeansException {

    return APPLICATION_CONTEXT.getBean(name);
  }
}
