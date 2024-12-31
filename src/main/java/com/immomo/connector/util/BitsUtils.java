package com.immomo.connector.util;


/**
 * @author zhang.bo
 * @since 07 24, 2017
 */
public final class BitsUtils {
  /** 判断{@code value}的第{@code position}位是否为1 */
  public static boolean isSet(byte value, int position) {
    return (value & (1 << position)) != 0;
  }

  /** 设置{@code value}的第{@code positions}位为1 */
  public static byte set(byte value, int... positions) {
    for (int position : positions) {
      value |= (1 << position);
    }

    return value;
  }

  public static void main(String[] args) {



  }
}
