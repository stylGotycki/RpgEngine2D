package net.dp.rpg.engine.puzzle;

public enum PuzzleKind {
  // A crystal on each side of a 5x3 board.
  CIRCUIT(7, 3),
  // A 4x3 board over a free row to step on from, and a reset plate at the bottom right.
  TOGGLE(5, 4),
  // Five plates every other tile, the start plate two rows below the middle one.
  SEQUENCE(9, 3);

  private final int width;

  private final int height;

  PuzzleKind(int width, int height) {
    this.width = width;
    this.height = height;
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }
}
