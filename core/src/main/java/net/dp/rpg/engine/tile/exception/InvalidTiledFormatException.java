package net.dp.rpg.engine.tile.exception;

public class InvalidTiledFormatException extends TileException {

  public InvalidTiledFormatException(String message) {
    super(message);
  }

  public InvalidTiledFormatException(String message, Throwable cause) {
    super(message, cause);
  }

  public static InvalidTiledFormatException unexpectedRoot(String expected, String actual, String path) {
    return new InvalidTiledFormatException("Expected <%s> root element but found <%s> in %s"
        .formatted(expected, actual, path));
  }

  public static InvalidTiledFormatException missingElement(String element, String path) {
    return new InvalidTiledFormatException("Missing <%s> element in %s".formatted(element, path));
  }

  public static InvalidTiledFormatException wrongPropertyType(String name, String expected,
                                                              Object value) {
    return new InvalidTiledFormatException("Property '%s' must be %s, got %s"
        .formatted(name, expected, value == null ? "nothing" : value.getClass().getSimpleName()));
  }
}
