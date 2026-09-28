package net.dp.rpg.engine.tile.exception;

public class InvalidTileMapException extends TileException {

  public InvalidTileMapException(String message) {
    super(message);
  }

  public static InvalidTileMapException sizeMismatch(String layerName, int width, int height, int expectedWidth,
                                                     int expectedHeight) {
    return new InvalidTileMapException("Layer '%s' is %dx%d, expected %dx%d"
        .formatted(layerName, width, height, expectedWidth, expectedHeight));
  }

  public static InvalidTileMapException duplicateLayer(String layerName) {
    return new InvalidTileMapException("Duplicate tile layer: " + layerName);
  }
}
