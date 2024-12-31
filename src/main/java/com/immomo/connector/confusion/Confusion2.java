package com.immomo.connector.confusion;

/**
 * 将字符串的前⾯2个字符和后⾯的两个字符互换 原始字符串：6KtlEPa6mctYG9eQgoW2pg== 变换后的字符串：==tlEPa6mctYG9eQgoW2pg6K
 *
 * @author sun.xusen@immomo.com
 * @date 2015-3-9
 */
public class Confusion2 implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {

    char[] charArray = source.toCharArray();
    char prefix1 = charArray[0];
    char prefix2 = charArray[1];
    char suffix1 = charArray[charArray.length - 2];
    char suffix2 = charArray[charArray.length - 1];
    charArray[0] = suffix1;
    charArray[1] = suffix2;
    charArray[charArray.length - 2] = prefix1;
    charArray[charArray.length - 1] = prefix2;
    return new String(charArray);
  }
}
