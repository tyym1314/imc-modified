package com.immomo.connector.exceptions;

public class BadMsgBytePacketException extends RuntimeException {

  /**
   * 
   */
  private static final long serialVersionUID = 1L;

  public BadMsgBytePacketException() {

  }

  public BadMsgBytePacketException(String message) {
    super(message);
  }

  public BadMsgBytePacketException(Throwable cause) {
    super(cause);
  }

  public BadMsgBytePacketException(String message, Throwable cause) {
    super(message, cause);
  }

}
