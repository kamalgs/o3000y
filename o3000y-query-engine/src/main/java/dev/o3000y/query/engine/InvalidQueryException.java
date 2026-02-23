package dev.o3000y.query.engine;

public final class InvalidQueryException extends RuntimeException {

  public InvalidQueryException(String message) {
    super(message);
  }

  public InvalidQueryException(String message, Throwable cause) {
    super(message, cause);
  }
}
