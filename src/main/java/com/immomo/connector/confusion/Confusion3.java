package com.immomo.connector.confusion;

/**
 * 将第三个字符换成a，倒数第四个字符换成Q 原始字符串：ULTsz+bmaNFCVoo3Hm063g== 变换后的字符串：ULasz+bmaNFCVoo3Hm06Qg==
 *
 * @author sun.xusen@immomo.com
 * @date 2015-3-9
 */
public class Confusion3 implements IConfusion {

  @Override
  public String confusion(String momoid, String source) {
    char[] charArray = source.toCharArray();
    charArray[2] = 'a';
    charArray[charArray.length - 4] = 'Q';
    return new String(charArray);
  }
}
