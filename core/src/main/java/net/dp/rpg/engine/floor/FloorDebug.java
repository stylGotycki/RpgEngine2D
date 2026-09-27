package net.dp.rpg.engine.floor;

import java.util.Arrays;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;

public final class FloorDebug {

  private static final String TRUNK_SYMBOLS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";

  private static final String APPENDIX_SYMBOLS = "abcdefghijklmnopqrstuvwxyz";

  private static final String HOLE_SYMBOLS = "0123456789";

  private static final char EMPTY = '.';

  private static final char GAP = ' ';

  private FloorDebug() {
  }

  public static String render(FloorGraph graph, GridBounds bounds) {
    int width = bounds.width() * 2 - 1;
    int height = bounds.height() * 2 - 1;
    char[][] canvas = blank(width, height);

    for (int y = 0; y < bounds.height(); y++) {
      for (int x = 0; x < bounds.width(); x++) {
        RoomCell cell = new RoomCell(x, y);
        RoomNode room = graph.roomAt(cell);

        canvas[y * 2][x * 2] = room == null ? EMPTY : symbolOf(room);

        if (room != null) {
          paintGaps(canvas, graph, room, cell);
        }
      }
    }

    return join(canvas);
  }

  public static String summary(FloorGraph graph) {
    graph.computeDepths();

    RoomNode deepest = graph.deepest();

    return "rooms=%d cells=%d doors=%d deadEnds=%d maxDepth=%d".formatted(
        graph.size(), graph.usedCells(), graph.links().size(), graph.deadEnds().size(),
        deepest == null ? 0 : deepest.depth());
  }

  private static void paintGaps(char[][] canvas, FloorGraph graph, RoomNode room, RoomCell cell) {
    paintGap(canvas, graph, room, cell, Direction.EAST, '-', '=');
    paintGap(canvas, graph, room, cell, Direction.SOUTH, '|', '"');
  }

  private static void paintGap(char[][] canvas, FloorGraph graph, RoomNode room, RoomCell cell,
                               Direction direction, char doorSymbol, char lockedSymbol) {
    RoomCell neighbour = cell.neighbour(direction);
    RoomNode other = graph.roomAt(neighbour);

    if (other == null) {
      return;
    }

    int gapX = cell.x() * 2 + direction.getDeltaX();
    int gapY = cell.y() * 2 + direction.getDeltaY();

    if (gapY >= canvas.length || gapX >= canvas[0].length) {
      return;
    }

    if (other == room) {
      canvas[gapY][gapX] = symbolOf(room);

      return;
    }

    RoomLink door = graph.doorAt(cell, direction);

    if (door != null) {
      canvas[gapY][gapX] = door.doorType() == DoorType.LOCKED ? lockedSymbol : doorSymbol;
    }
  }

  private static char symbolOf(RoomNode room) {
    String alphabet = switch (room.phase()) {
      case TRUNK -> TRUNK_SYMBOLS;
      case APPENDIX -> APPENDIX_SYMBOLS;
      case HOLE -> HOLE_SYMBOLS;
    };

    return alphabet.charAt(room.index() % alphabet.length());
  }

  private static char[][] blank(int width, int height) {
    char[][] canvas = new char[height][width];

    for (char[] row : canvas) {
      Arrays.fill(row, GAP);
    }

    return canvas;
  }

  private static String join(char[][] canvas) {
    StringBuilder builder = new StringBuilder();

    for (char[] row : canvas) {
      builder.append(new String(row).stripTrailing()).append(System.lineSeparator());
    }

    return builder.toString();
  }
}
