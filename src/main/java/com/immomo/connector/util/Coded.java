package com.immomo.connector.util;

import java.io.UnsupportedEncodingException;

/**
 * Created by wudongyue on 15/9/17.
 */
public class Coded {
  static {
    //libpltcoded.so
    System.loadLibrary("pltcoded");
    System.out.println("LOAD DONE ");
  }

  public static void main(String[] args) {
    System.out.println("DONE ");
  }

  private static char[] base64EncodeChars = new char[] {
      'A', 'B', 'C', 'D', 'E', 'F', 'G', 'H',
      'I', 'J', 'K', 'L', 'M', 'N', 'O', 'P',
      'Q', 'R', 'S', 'T', 'U', 'V', 'W', 'X',
      'Y', 'Z', 'a', 'b', 'c', 'd', 'e', 'f',
      'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n',
      'o', 'p', 'q', 'r', 's', 't', 'u', 'v',
      'w', 'x', 'y', 'z', '0', '1', '2', '3',
      '4', '5', '6', '7', '8', '9', '+', '/' };

  private static byte[] base64DecodeChars = new byte[] {
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
      -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, 62, -1, -1, -1, 63,
      52, 53, 54, 55, 56, 57, 58, 59, 60, 61, -1, -1, -1, -1, -1, -1,
      -1,  0,  1,  2,  3,  4,  5,  6,  7,  8,  9, 10, 11, 12, 13, 14,
      15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25, -1, -1, -1, -1, -1,
      -1, 26, 27, 28, 29, 30, 31, 32, 33, 34, 35, 36, 37, 38, 39, 40,
      41, 42, 43, 44, 45, 46, 47, 48, 49, 50, 51, -1, -1, -1, -1, -1 };

  public static String encode(byte[] data) {
    StringBuffer sb = new StringBuffer();
    int len = data.length;
    int i = 0;
    int b1, b2, b3;
    while (i < len) {
      b1 = data[i++] & 0xff;
      if (i == len)
      {
        sb.append(base64EncodeChars[b1 >>> 2]);
        sb.append(base64EncodeChars[(b1 & 0x3) << 4]);
        sb.append("==");
        break;
      }
      b2 = data[i++] & 0xff;
      if (i == len)
      {
        sb.append(base64EncodeChars[b1 >>> 2]);
        sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
        sb.append(base64EncodeChars[(b2 & 0x0f) << 2]);
        sb.append("=");
        break;
      }
      b3 = data[i++] & 0xff;
      sb.append(base64EncodeChars[b1 >>> 2]);
      sb.append(base64EncodeChars[((b1 & 0x03) << 4) | ((b2 & 0xf0) >>> 4)]);
      sb.append(base64EncodeChars[((b2 & 0x0f) << 2) | ((b3 & 0xc0) >>> 6)]);
      sb.append(base64EncodeChars[b3 & 0x3f]);
    }
    return sb.toString();
  }

  public static byte[] decode(String str) throws UnsupportedEncodingException {
    StringBuffer sb = new StringBuffer();
    byte[] data = str.getBytes("US-ASCII");
    int len = data.length;
    int i = 0;
    int b1, b2, b3, b4;
    while (i < len) {
                /* b1 */
      do {
        b1 = base64DecodeChars[data[i++]];
      } while (i < len && b1 == -1);
      if (b1 == -1) break;
                /* b2 */
      do {
        b2 = base64DecodeChars[data[i++]];
      } while (i < len && b2 == -1);
      if (b2 == -1) break;
      sb.append((char)((b1 << 2) | ((b2 & 0x30) >>> 4)));
                /* b3 */
      do {
        b3 = data[i++];
        if (b3 == 61) return sb.toString().getBytes("ISO-8859-1");
        b3 = base64DecodeChars[b3];
      } while (i < len && b3 == -1);
      if (b3 == -1) break;
      sb.append((char)(((b2 & 0x0f) << 4) | ((b3 & 0x3c) >>> 2)));
                /* b4 */
      do {
        b4 = data[i++];
        if (b4 == 61) return sb.toString().getBytes("ISO-8859-1");
        b4 = base64DecodeChars[b4];
      } while (i < len && b4 == -1);
      if (b4 == -1) break;
      sb.append((char)(((b3 & 0x03) << 6) | b4));
    }
    return sb.toString().getBytes("ISO-8859-1");
  }

  /**
   * 客户端 shared_secret 生成。
   * @param sharedSecret
   * @param serverPubkey
   * @param serverPubkeyLen
   * @param clientPubkey
   * @param group
   * @param groupLen
   * @return 客户端 public_ken 长度
   */
  public static native int clientSecretGen(byte [] sharedSecret, byte[] serverPubkey, int serverPubkeyLen, byte[] clientPubkey);

  /**
   * 服务器 shared_secret 生成。
   * @param sharedSecret
   * @param serverPubkey
   * @param serverPubkeyLen
   * @param clientPubkey
   * @param clientPubkeyLen
   * @param group
   * @param groupLen
   * @return
   */
  public static native int serverSecretGen(byte [] sharedSecret, byte[] clientPubkey, int clientPubkeyLen,int version);

  /**
   * aes封装:
   * ilen为输入明文长度，
   * aes 包封装格式
   * 输出加密后密文长度。
   * @param input
   * @param inputLen
   * @param key
   * @param output
   * @return 加密之后需要在output取出数据的实际长度（一般会小于output.length）
   */
  public static native int aesEncode(byte[] input, int inputLen, byte[] key, int klen,byte[] output);

  /**
   * aes解封装:
   * ilen为输入密文长度，
   * 输出解密后明文长度。
   * @param input
   * @param inputLen
   * @param key
   * @param output
   * @return 解密之后需要在output取出数据的实际长度（一般会小于output.length）
   */
  public static native int aesDecode(byte[] input, int inputLen, byte[] key, int klen,byte[] output);



  /**
   * 根据输入长度和函数类型计算用作初始化的输出长度。注，这里输出长度只是用作输出内存初始化，并不一定等于最后输出长度。
   * @param inputLen
   * @param type
   * @return
   */
  public static native int computeOutputLength(int inputLen, int type);



//    public static void main(String[] args){
//        try {
//
//            String data = "这是一条加密测试数据！";
//            byte[] origByte = data.getBytes();
//
//            final int SERVER_PUBLIC_KEY_LENGTH = computeOutputLength(0, 6 /* PUBLIC_KEY_CURVE192 */);
//            final int SERVER_SECRET_LENGTH = computeOutputLength(0,7 /* KEY_CURVE192 */);
//            final int CLIENT_PUBLIC_KEY_LENGTH = computeOutputLength(0, 6 /* PUBLIC_KEY_CURVE192 */);
//            final int GROUP_LENGTH = computeOutputLength(0, 5 /* GROUP_CURVE192 */);
//            final int SHARED_SECRET_LENGTH = 24;
//            final int AES_KEY_LENGTH = 16;
//
//            // 获取服务器公钥
//            byte[] ServerPK = getServerPK(1);
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll getServerPK:" + encode(ServerPK));
//
//            // 获取服务器私钥
//            byte[] ServerSK = getServerSK(1);
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll getServersK:" + encode(ServerSK));
//
//            // 获取加密参数
//            byte[] group = getGroup(1);
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll getGroup:" + encode(group));
//
//            byte[] sharedSecret = new byte[SHARED_SECRET_LENGTH];
//            byte[] clientPubkey = new byte[CLIENT_PUBLIC_KEY_LENGTH];
//
//            // 生成 客户端sharedSecret 和 客户端公钥
//            int result = clientSecretGen(sharedSecret,ServerPK,ServerPK.length,clientPubkey,group,group.length);
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll clientPubkey:" + encode(clientPubkey) +"    pkLen:"+result);
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll sharedSecret:" + encode(sharedSecret));
//
//            // 从sharedSecret截取出AesKey
//            byte[] clentAesKey = new byte[AES_KEY_LENGTH];
//            for (int i = 0; i < AES_KEY_LENGTH; i++) {
//                clentAesKey[i] = sharedSecret[i];
//            }
//            int aesenc_len= computeOutputLength(origByte.length, 1 /* AES_ENC */);
//            byte[] encOutputByte = new byte[aesenc_len];
//            // 使用截取出来的AesKey加密数据
//            int aesOutPutLen = aesEncode(origByte, origByte.length, clentAesKey, clentAesKey.length,encOutputByte);
//            byte[] aesEncodeByte = new byte[aesOutPutLen];
//            for (int i = 0; i < aesOutPutLen; i++) {
//                aesEncodeByte[i] = encOutputByte[i];
//            }
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll aesEncode:" + encode(aesEncodeByte));
//
//            // 解密数据
//            int aesdec_len= computeOutputLength(aesEncodeByte.length, 2 /* AES_DEC */);
//            byte[] decOutputByte = new byte[aesdec_len];
//            int decOutputLen = aesDecode(aesEncodeByte,aesEncodeByte.length,clentAesKey,clentAesKey.length,decOutputByte);
//            byte[] aesDecodeByte = new byte[decOutputLen];
//            for (int i = 0; i < decOutputLen; i++) {
//                aesDecodeByte[i] = decOutputByte[i];
//            }
////            byte aesDecode
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll aesDecode:" + new String(aesDecodeByte));
//
//            // 生成 服务器 sharedSecret
//            byte[] serverSharedSecret = new byte[SHARED_SECRET_LENGTH];
//            serverSecretGen(serverSharedSecret,ServerSK,SERVER_SECRET_LENGTH,clientPubkey,result,group,GROUP_LENGTH);
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll serverSharedSecret:" + encode(serverSharedSecret));
//
//            // 截取出AesKey & 服务器解密客户端数据
//            byte[] serverAesKey = new byte[AES_KEY_LENGTH];
//            for (int i = 0; i < AES_KEY_LENGTH; i++) {
//                serverAesKey[i] = serverSharedSecret[i];
//            }
//            int serverDecodeLen = computeOutputLength(encOutputByte.length, 2 /* AES_DEC */);
//            byte[] serverDecOuputByte = new byte[serverDecodeLen];
//            int serverDecOutputLen = aesDecode(aesEncodeByte,aesEncodeByte.length,serverAesKey,serverAesKey.length,serverDecOuputByte);
//            byte[] serverAesDecodeByte = new byte[serverDecOutputLen];
//            for (int i = 0; i < serverDecOutputLen; i++) {
//                serverAesDecodeByte[i] = serverDecOuputByte[i];
//            }
//            System.out.println("@@@@@@@@@@@@@@@@@ testAll serverAesDecode:" + new String(serverAesDecodeByte));
//
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }


}
