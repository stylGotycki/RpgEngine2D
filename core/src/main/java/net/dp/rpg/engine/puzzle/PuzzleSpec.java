package net.dp.rpg.engine.puzzle;

import java.util.ArrayList;
import java.util.List;

// A puzzle laid out in a room: its area in local tiles (x, y is the bottom-left corner) and the
// seed its board is generated from. questId is null for a puzzle room's own puzzle.
public record PuzzleSpec(PuzzleKind kind, int x, int y, long seed, String questId) {

  public static final int CIRCUIT_BOARD_WIDTH = 5;

  public static final int TOGGLE_BOARD_WIDTH = 4;

  public static final int TOGGLE_BOARD_HEIGHT = 3;

  public static final int SEQUENCE_PLATES = 5;

  public int width() {
    return kind.width();
  }

  public int height() {
    return kind.height();
  }

  public boolean contains(int tileX, int tileY) {
    return tileX >= x && tileY >= y && tileX < x + width() && tileY < y + height();
  }

  public List<int[]> tiles() {
    List<int[]> tiles = new ArrayList<>();

    for (int dy = 0; dy < height(); dy++) {
      for (int dx = 0; dx < width(); dx++) {
        tiles.add(new int[] {x + dx, y + dy});
      }
    }

    return tiles;
  }

  public int[] circuitCell(int column, int row) {
    return new int[] {x + 1 + column, y + row};
  }

  public int[] toggleCell(int column, int row) {
    return new int[] {x + column, y + 1 + row};
  }

  public int[] toggleReset() {
    return new int[] {x + TOGGLE_BOARD_WIDTH, y};
  }

  public int[] sequencePlate(int index) {
    return new int[] {x + 2 * index, y + 2};
  }

  public int[] sequenceStart() {
    return new int[] {x + 4, y};
  }
}
