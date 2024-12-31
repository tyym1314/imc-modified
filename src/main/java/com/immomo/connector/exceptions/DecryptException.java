package com.immomo.connector.exceptions;


/**
 * 解密客户端的包时发生错误
 */
public class DecryptException extends RuntimeException {

  /**
   *
   */
  private static final long serialVersionUID = 1L;

  public DecryptException() {}

  public DecryptException(String message) {
    super(message);
  }

  public DecryptException(Throwable cause) {
    super(cause);
  }

  public DecryptException(String message, Throwable cause) {
    super(message, cause);
  }
}
