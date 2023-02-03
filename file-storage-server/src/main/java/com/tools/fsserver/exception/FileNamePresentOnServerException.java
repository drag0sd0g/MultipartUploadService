package com.tools.fsserver.exception;

public class FileNamePresentOnServerException extends Exception {
  public FileNamePresentOnServerException(String message) {
    super(message);
  }
}
