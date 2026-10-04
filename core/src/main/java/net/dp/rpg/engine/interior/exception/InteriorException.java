package net.dp.rpg.engine.interior.exception;

public abstract class InteriorException extends RuntimeException {

  protected InteriorException(String message) {
    super(message);
  }

  protected InteriorException(String message, Throwable cause) {
    super(message, cause);
  }
}
