package net.dp.rpg.engine.interior;

import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.wfc.WfcState;


public record GeneratedRoom(
    RoomBlueprint blueprint,
    RoomCanvas canvas,
    WfcState ground,
    TileMapData map,
    Status status,
    int roomAttempts,
    String note) {

  public enum Status {

    GENERATED,
    FALLBACK
  }

  public boolean isFallback() {
    return status == Status.FALLBACK;
  }

  public boolean wasRetried() {
    return status == Status.GENERATED && roomAttempts > 1;
  }
}
