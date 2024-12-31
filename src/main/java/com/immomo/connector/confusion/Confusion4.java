package com.immomo.connector.confusion;

/**
 * 在字符串的第七个字符后插⼊字符串a46MXPbc5A 原始字符串：KIBCA8SYL2YGZOF8L5Dtyw== 变换后的字符串：KIBCA8Sa46MXPbc5AYL2YGZOF8L5Dtyw==
 *
 * @author sun.xusen@immomo.com
 * @date 2015-3-9
 */
public class Confusion4 implements IConfusion {

  private static final String _FILL = "a46MXPbc5A";

  @Override
  public String confusion(String momoid, String source) {
    return new StringBuilder().append(source.substring(0, 7)).append(_FILL)
        .append(source.substring(7, source.length())).toString();
  }
}
