package net.dp.rpg.engine.floor.shape;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class ShapeHoles {

  private ShapeHoles() {
  }

  public static List<List<RoomCell>> find(RoomShape shape) {
    Set<RoomCell> outside = floodOutside(shape);
    List<RoomCell> enclosed = new ArrayList<>();

    for (int y = 0; y < shape.cellsDown(); y++) {
      for (int x = 0; x < shape.cellsAcross(); x++) {
        RoomCell cell = new RoomCell(x, y);

        if (!shape.contains(cell) && !outside.contains(cell)) {
          enclosed.add(cell);
        }
      }
    }

    return groupRegions(enclosed);
  }

  private static Set<RoomCell> floodOutside(RoomShape shape) {
    Set<RoomCell> outside = new LinkedHashSet<>();
    Deque<RoomCell> pending = new ArrayDeque<>();
    RoomCell corner = new RoomCell(-1, -1);

    outside.add(corner);
    pending.add(corner);

    while (!pending.isEmpty()) {
      RoomCell current = pending.removeFirst();

      for (Direction direction : Direction.values()) {
        RoomCell neighbour = current.neighbour(direction);

        if (inExpandedBox(shape, neighbour) && !shape.contains(neighbour) && outside.add(neighbour)) {
          pending.addLast(neighbour);
        }
      }
    }

    return outside;
  }

  private static boolean inExpandedBox(RoomShape shape, RoomCell cell) {
    return cell.x() >= -1 && cell.x() <= shape.cellsAcross()
        && cell.y() >= -1 && cell.y() <= shape.cellsDown();
  }

  private static List<List<RoomCell>> groupRegions(List<RoomCell> cells) {
    Set<RoomCell> remaining = new LinkedHashSet<>(cells);
    List<List<RoomCell>> regions = new ArrayList<>();

    while (!remaining.isEmpty()) {
      RoomCell seed = remaining.iterator().next();
      List<RoomCell> region = new ArrayList<>();
      Deque<RoomCell> pending = new ArrayDeque<>();

      remaining.remove(seed);
      region.add(seed);
      pending.add(seed);

      while (!pending.isEmpty()) {
        RoomCell current = pending.removeFirst();

        for (Direction direction : Direction.values()) {
          RoomCell neighbour = current.neighbour(direction);

          if (remaining.remove(neighbour)) {
            region.add(neighbour);
            pending.addLast(neighbour);
          }
        }
      }

      regions.add(List.copyOf(region));
    }

    return List.copyOf(regions);
  }
}
