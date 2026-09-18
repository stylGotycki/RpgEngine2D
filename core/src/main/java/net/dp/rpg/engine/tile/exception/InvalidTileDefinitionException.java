package net.dp.rpg.engine.tile.exception;

public class InvalidTileDefinitionException extends TileException {

  public InvalidTileDefinitionException(String message) {
    super(message);
  }

  public InvalidTileDefinitionException(String message, Throwable cause) {
    super(message, cause);
  }

  public static InvalidTileDefinitionException blankField(String field) {
    return new InvalidTileDefinitionException(field + " must not be blank");
  }

  public static InvalidTileDefinitionException unknownEnumValue(String field, String value, Throwable cause) {
    return new InvalidTileDefinitionException("Unknown %s: '%s'".formatted(field, value), cause);
  }
}
