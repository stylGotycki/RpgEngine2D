package net.dp.rpg.engine.interior.pass;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.model.WalkableClasses;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

final class SpineTargets {

  private SpineTargets() {
  }

  static List<int[]> of(RoomCanvas canvas, WalkableClasses walkable,
      List<TileMapObject> extraPoints) {
    List<int[]> targets = new ArrayList<>();

    for (RoomEdge edge : canvas.blueprint().doors().keySet()) {
      targets.add(approachOf(edge));
    }

    for (TileMapObject point : extraPoints) {
      targets.add(new int[] {point.tileX(), point.tileY()});
    }

    if (targets.size() == 1) {
      int[] hub = hubOf(canvas, walkable, targets.getFirst());

      if (hub != null) {
        targets.add(hub);
      }
    }

    return targets;
  }

  static int[] approachOf(RoomEdge edge) {
    Direction inwards = edge.direction().opposite();

    return new int[] {edge.doorTileX() + inwards.getDeltaX(),
        edge.doorTileY() + inwards.getDeltaY()};
  }

  private static int[] hubOf(RoomCanvas canvas, WalkableClasses walkable, int[] other) {
    RoomCell cell = canvas.blueprint().doors().keySet().stream()
        .map(RoomEdge::cell)
        .findFirst()
        .orElse(canvas.blueprint().shape().anchorCell());
    int startX = cell.centerTileX();
    int startY = cell.centerTileY();
    boolean[] seen = new boolean[canvas.width() * canvas.height()];
    Deque<int[]> pending = new ArrayDeque<>();

    pending.add(new int[] {startX, startY});
    seen[startX + startY * canvas.width()] = true;

    while (!pending.isEmpty()) {
      int[] tile = pending.removeFirst();
      boolean isOther = tile[0] == other[0] && tile[1] == other[1];

      if (!isOther && canHoldHub(canvas, walkable, tile[0], tile[1])) {
        return tile;
      }

      for (Direction direction : Direction.values()) {
        int x = tile[0] + direction.getDeltaX();
        int y = tile[1] + direction.getDeltaY();

        if (canvas.zones().isInside(x, y) && !seen[x + y * canvas.width()]) {
          seen[x + y * canvas.width()] = true;
          pending.addLast(new int[] {x, y});
        }
      }
    }

    return null;
  }

  private static boolean canHoldHub(RoomCanvas canvas, WalkableClasses walkable, int x, int y) {
    return canvas.zones().zoneAt(x, y) == Zone.INTERIOR
        && !canvas.ground().isFixed(x, y)
        && !walkable.walkableSubsetOf(canvas.ground().startingDomain(x, y)).isEmpty();
  }
}
