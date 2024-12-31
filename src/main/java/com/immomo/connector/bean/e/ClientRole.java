package com.immomo.connector.bean.e;

/**
 * 区分各端
 *
 * @author zhang.bo
 * @since 04 08, 2019
 */
public enum ClientRole {
  // 直播ios & android
  Momo_Live(1),
  // hani
  Hani_live(2),
  // third all
  Third_Live(3)
  ;

  private int value;

  ClientRole(int role) {
    this.value = role;
  }

  public static ClientRole forRole(Integer role) {
    if (role != null) {
      switch (role) {
        case 0:
        case 1:
          return Momo_Live;
        case 2:
          return Hani_live;
        case 3:
          return Third_Live;
        default:
//          throw new IllegalArgumentException("illegal role value " + role);
          return Momo_Live;
      }
    }

//    throw new IllegalArgumentException("illegal role value null");
    return Momo_Live;
  }

  public int getValue() {
    return value;
  }
}
