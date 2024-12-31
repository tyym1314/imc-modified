package com.immomo.connector.decrypt.impl;

import com.google.common.base.Charsets;
import com.immomo.connector.util.Coded;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LiveAESV3 {

  public static byte[] decode(byte[] b, String aesKey) {
    // decode为2
//    log.info("**************decode debug,b[" + Arrays.toString(b) + "],aesKey[" + aesKey + "]");
    int jniApplyLen = Coded.computeOutputLength(b.length, 2);
    byte[] jniApply = new byte[jniApplyLen];

    byte[] bytes = aesKey.getBytes(Charsets.UTF_8);
    int realLen = Coded.aesDecode(b, b.length, bytes, bytes.length, jniApply);

    return copy(jniApplyLen, jniApply, realLen);
  }

  static byte[] copy(int jniApplyLen, byte[] jniApply, int realLen) {
    if (realLen == jniApplyLen) {
      return jniApply;
    }

    byte[] real = new byte[realLen];
    System.arraycopy(jniApply, 0, real, 0, realLen);
    return real;
  }

  public static byte[] encode(byte[] b, String aesKey) {
    // encode为1
    int jniApplyLen = Coded.computeOutputLength(b.length, 1);
    byte[] jniApply = new byte[jniApplyLen];
    byte[] bytes = aesKey.getBytes(Charsets.UTF_8);
    int realLen = Coded.aesEncode(b, b.length, bytes, bytes.length, jniApply);

    return copy(jniApplyLen, jniApply, realLen);
  }
}
