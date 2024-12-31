package com.immomo.connector;

import com.immomo.env.MomoEnv;
import org.springframework.context.support.ClassPathXmlApplicationContext;

import java.util.Properties;

public class LiveImBootstrap {

  public static void main(String[] args) {
    boolean isDev = "alpha".equals(MomoEnv.corp());
    ClassPathXmlApplicationContext context = new ClassPathXmlApplicationContext(
            !isDev ? "classpath*:spring-root.xml" : "classpath*:spring-root-dev.xml");
    context.registerShutdownHook();
    
  }

}
