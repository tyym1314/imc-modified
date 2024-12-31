package com.immomo.connector.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.UnknownHostException;
import java.util.Enumeration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created By wlb on 2019-10-22 19:30
 */
public class ImIpUtils {

  private final static Logger LOG = LoggerFactory.getLogger(ImIpUtils.class);

  public static final String IP_LAN = getIpLAN();

  public static final String IP_WAN = getIpWAN();

  public static final String HOST_NAME = getHostName();


  private static String getIpLAN() {
    try {
      Enumeration<NetworkInterface> netInterfaces;
      netInterfaces = NetworkInterface.getNetworkInterfaces();
      while (netInterfaces.hasMoreElements()) {
        NetworkInterface ni = netInterfaces.nextElement();
        Enumeration<InetAddress> ips = ni.getInetAddresses();
        while (ips.hasMoreElements()) {
          String ip = ips.nextElement().getHostAddress();
          if (ip.startsWith("192.168.") || ip.startsWith("10.")) {
            return ip;
          }
        }
      }
    } catch (Exception e) {
      LOG.error("getIpLAN error!", e);
    }
    return "127.0.0.1";
  }

  private static String getHostName() {
    try {
      return InetAddress.getLocalHost().getHostName();
    } catch (UnknownHostException e) {
      LOG.error("getHostName error!", e);
    }
    return "localhost";
  }

  /**
   * support multi network-card, useful in linux env
   */
  private static String getIpWAN() {
    try {
      Enumeration<NetworkInterface> netInterfaces = null;
      netInterfaces = NetworkInterface.getNetworkInterfaces();
      while (netInterfaces.hasMoreElements()) {
        NetworkInterface ni = netInterfaces.nextElement();
        Enumeration<InetAddress> ips = ni.getInetAddresses();
        while (ips.hasMoreElements()) {
          String ip = ips.nextElement().getHostAddress();
          // 过滤针对阿里高防添加的新ip
          if (ip.startsWith("1.1.1")) {
            continue;
          }

//          if (ip.startsWith("")) {
          if (ip.startsWith("2.2.2")) {
            return ip;
          }

          if (ip.startsWith("3.3.3")) {
            return ip;
          }
        }
      }
    } catch (Exception e) {
      LOG.error("getIpWAN error!", e);
    }
    return "127.0.0.1";
  }

}
