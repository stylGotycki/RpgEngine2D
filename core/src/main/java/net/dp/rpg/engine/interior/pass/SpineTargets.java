package net.dp.rpg.engine.interior.pass;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.RoomEdge;

final class SpineTargets {

  private SpineTargets() {
  }

  static List<int[]> of(RoomBlueprint blueprint, List<TileMapObject> extraPoints) {
    List<int[]> targets = new ArrayList<>();

    for (RoomEdge edge : blueprint.doors().keySet()) {
      int approachX = edge.doorTileX() + edge.direction().opposite().getDeltaX();
      int approachY = edge.doorTileY() + edge.direction().opposite().getDeltaY();

      targets.add(new int[] {approachX, approachY});
    }

    for (TileMapObject point : extraPoints) {
      targets.add(new int[] {(int) Math.floor(point.x()), (int) Math.floor(point.y())});
    }

    return targets;
  }
}
