package net.dp.rpg.engine.tile.exception;

public class InvalidTilesetException extends TileException {

  public InvalidTilesetException(String message) {
    super(message);
  }

  public static InvalidTilesetException blankField(String field) {
    return new InvalidTilesetException(field + " must not be blank");
  }

  public static InvalidTilesetException nonPositive(String field, int value) {
    return new InvalidTilesetException("%s must be greater than zero, got %d".formatted(field, value));
  }

  public static InvalidTilesetException localIdOutOfRange(String tilesetId, String tileTypeId, int localId,
                                                          int tileCount) {
    return new InvalidTilesetException("Tileset '%s' binds type '%s' to local id %d, but holds only %d tiles"
        .formatted(tilesetId, tileTypeId, localId, tileCount));
  }
}
