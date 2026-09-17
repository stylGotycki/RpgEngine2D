package net.dp.rpg.engine.tile.room;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.dp.rpg.engine.tile.exception.InvalidRoomShapeException;

public record RoomShape(Set<RoomCell> cells, int cellsAcross, int cellsDown) {

  public RoomShape {
    if (cells == null || cells.isEmpty()) {
      throw new InvalidRoomShapeException("Room shape must contain at least one cell");
    }

    cells = Set.copyOf(cells);
  }

  public static RoomShape of(Collection<RoomCell> cells) {
    if (cells == null || cells.isEmpty()) {
      throw new InvalidRoomShapeException("Room shape must contain at least one cell");
    }

    Set<RoomCell> normalised = normalise(cells);

    requireConnected(normalised);

    int across = normalised.stream().mapToInt(RoomCell::x).max().orElseThrow() + 1;
    int down = normalised.stream().mapToInt(RoomCell::y).max().orElseThrow() + 1;

    return new RoomShape(normalised, across, down);
  }

  public static RoomShape single() {
    return of(List.of(RoomCell.ORIGIN));
  }

  public static RoomShape parse(String... rows) {
    List<RoomCell> cells = new ArrayList<>();

    for (int y = 0; y < rows.length; y++) {
      String row = rows[y];

      for (int x = 0; x < row.length(); x++) {
        char symbol = row.charAt(x);

        if (symbol != ' ' && symbol != '.') {
          cells.add(new RoomCell(x, y));
        }
      }
    }

    return of(cells);
  }

  public boolean contains(RoomCell cell) {
    return cells.contains(cell);
  }

  public int size() {
    return cells.size();
  }

  public int tileWidth() {
    return RoomGeometry.tileWidth(cellsAcross);
  }

  public int tileHeight() {
    return RoomGeometry.tileHeight(cellsDown);
  }

  public List<RoomEdge> outerEdges() {
    List<RoomEdge> edges = new ArrayList<>();

    for (RoomCell cell : cells) {
      for (Direction direction : Direction.values()) {
        if (!contains(cell.neighbour(direction))) {
          edges.add(new RoomEdge(cell, direction));
        }
      }
    }

    return edges;
  }

  private static Set<RoomCell> normalise(Collection<RoomCell> cells) {
    int minX = cells.stream().mapToInt(RoomCell::x).min().orElseThrow();
    int minY = cells.stream().mapToInt(RoomCell::y).min().orElseThrow();

    Set<RoomCell> normalised = new LinkedHashSet<>();

    cells.forEach(cell -> normalised.add(new RoomCell(cell.x() - minX, cell.y() - minY)));

    return normalised;
  }

  private static void requireConnected(Set<RoomCell> cells) {
    Deque<RoomCell> pending = new ArrayDeque<>();
    Set<RoomCell> seen = new LinkedHashSet<>();

    RoomCell start = cells.iterator().next();

    pending.add(start);
    seen.add(start);

    while (!pending.isEmpty()) {
      RoomCell current = pending.removeFirst();

      for (Direction direction : Direction.values()) {
        RoomCell neighbour = current.neighbour(direction);

        if (cells.contains(neighbour) && seen.add(neighbour)) {
          pending.addLast(neighbour);
        }
      }
    }

    if (seen.size() != cells.size()) {
      throw new InvalidRoomShapeException(
          "Room shape is not connected: %d of %d cells are unreachable"
              .formatted(cells.size() - seen.size(), cells.size()));
    }
  }
}
