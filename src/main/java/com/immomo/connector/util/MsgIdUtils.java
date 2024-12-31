package com.immomo.connector.util;

import java.util.UUID;

/**
 * Created By wlb on 2019-10-22 19:29
 */
public class MsgIdUtils {
  public static String id(){
    return UUID.randomUUID().toString().substring(0, 16).toLowerCase();
  }
}
