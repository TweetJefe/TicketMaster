package com.ticket.master.common.exception;

public class ServerException extends RuntimeException {
  public ServerException() {
    super("Server error");
  }
}
