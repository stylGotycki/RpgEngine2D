package net.dp.rpg.engine.interior.pass;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.List;
import java.util.Optional;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileRole;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.room.Direction;

public final class RoomReachability {

  private final TileTypeRegistry types;

  public RoomReachability(TileTypeRegistry types) {
    if (types == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    this.types = types;
  }

  public Optional<String> problem(RoomCanvas canvas, TileMapData map) {
    List<int[]> connectors = canvas.connectors();

    if (connectors.isEmpty()) {
      return Optional.empty();
    }

    for (int[] tile : connectors) {
      if (!isWalkable(map, tile[0], tile[1])) {
        return Optional.of("tile %d,%d the spine joined is not walkable".formatted(tile[0],
            tile[1]));
      }
    }

    boolean[] reached = flood(map, connectors.getFirst());

    for (int[] tile : connectors) {
      if (!reached[tile[0] + tile[1] * map.width()]) {
        return Optional.of("tile %d,%d cannot be reached from %d,%d".formatted(tile[0], tile[1],
            connectors.getFirst()[0], connectors.getFirst()[1]));
      }
    }

    return Optional.empty();
  }

  private boolean[] flood(TileMapData map, int[] start) {
    boolean[] reached = new boolean[map.width() * map.height()];
    Deque<int[]> pending = new ArrayDeque<>();

    reached[start[0] + start[1] * map.width()] = true;
    pending.add(start);

    while (!pending.isEmpty()) {
      int[] tile = pending.removeFirst();

      for (Direction direction : Direction.values()) {
        int x = tile[0] + direction.getDeltaX();
        int y = tile[1] + direction.getDeltaY();
        boolean inside = x >= 0 && y >= 0 && x < map.width() && y < map.height();

        if (inside && !reached[x + y * map.width()] && isWalkable(map, x, y)) {
          reached[x + y * map.width()] = true;
          pending.addLast(new int[] {x, y});
        }
      }
    }

    return reached;
  }

  public boolean isWalkable(TileMapData map, int x, int y) {
    int ground = map.requireLayer(TileLayerKind.GROUND).grid().getOrEmpty(x, y);

    if (ground == TileGrid.EMPTY) {
      return false;
    }

    TileType groundType = types.require(ground);

    if (groundType.role() == TileRole.DOOR) {
      return true;
    }

    if (!groundType.walkable()) {
      return false;
    }

    int detail = map.findLayer(TileLayerKind.DETAILS)
        .map(layer -> layer.grid().getOrEmpty(x, y))
        .orElse(TileGrid.EMPTY);

    return detail == TileGrid.EMPTY || types.require(detail).walkable();
  }
}
