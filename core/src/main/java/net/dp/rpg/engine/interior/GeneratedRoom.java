package net.dp.rpg.engine.interior;

import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.wfc.WfcState;

/**
 * One room's finished interior and how it came about. {@code blueprint} is the one actually used,
 * so after a retry its seed differs from the one first asked for; {@code canvas} keeps the zones
 * and the reserved route for overlays and later passes. For a {@link Status#FALLBACK} room there
 * is no solved ground ({@code ground} is null) and {@code note} says why generation gave up.
 */
public record GeneratedRoom(
    RoomBlueprint blueprint,
    RoomCanvas canvas,
    WfcState ground,
    TileMapData map,
    Status status,
    int roomAttempts,
    String note) {

  public enum Status {

    /** Prefabs, spine, WFC and details all succeeded. */
    GENERATED,

    /** Every attempt failed; the room is a plain walled room with its doors, so the floor holds. */
    FALLBACK
  }

  public boolean isFallback() {
    return status == Status.FALLBACK;
  }

  /** Whether a first attempt failed and a reseeded one succeeded. */
  public boolean wasRetried() {
    return status == Status.GENERATED && roomAttempts > 1;
  }
}
