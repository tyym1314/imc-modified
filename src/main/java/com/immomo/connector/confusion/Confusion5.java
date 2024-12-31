package com.immomo.connector.confusion;

/**
 * 将字符串的range（6，4）的⼦字符串（从第七个字符开始的4个字符 ⻓度的⼦字符串）和range（0，5）的字符串交换顺序，并在中间插⼊固 定字符串：+F4SKzTSv7i
 * 原始字符串：4ZV0hRuZT+zZmjR3UoSh4g== 变换后的字符串：uZT++F4SKzTSv7i4ZV0hzZmjR3UoSh4g==
 *
 * @author sun.xusen@immomo.com
 * @date 2015-3-9
 */
public class Confusion5 implements IConfusion {

  private static final String _FILL = "+F4SKzTSv7i";

  @Override
  public String confusion(String momoid, String source) {

    return new StringBuilder().append(source.substring(6, 10)).append(_FILL)
        .append(source.substring(0, 5)).append(source.substring(10)).toString();
  }
}
