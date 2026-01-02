package com.kcjmowright.exceptions;

import org.slf4j.helpers.MessageFormatter;

public class ResourceNotAvailableException extends RuntimeException {

  public ResourceNotAvailableException(String message) {
    super(message);
  }

  public ResourceNotAvailableException(String message, Object... args) {
    super(formatMessage(message, args));
  }

  public ResourceNotAvailableException(Throwable cause, String message, Object... args) {
      super(formatMessage(message, args), cause);
  }

  private static String formatMessage(String message, Object... args) {
    if (args == null || args.length == 0) {
      return message;
    }
    return MessageFormatter.arrayFormat(message, args).getMessage();
  }

}
