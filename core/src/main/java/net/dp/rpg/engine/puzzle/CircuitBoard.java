package net.dp.rpg.engine.puzzle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

// Pipes on a grid: the beam enters through the left edge at sourceRow and must
// leave the right edge at sinkRow. Rotating a piece turns it 90 degrees clockwise.
public final class CircuitBoard {

  public static final int NORTH = 1;

  public static final int EAST = 2;

  public static final int SOUTH = 4;

  public static final int WEST = 8;

  public enum Piece {
    STRAIGHT(NORTH | SOUTH),
    CORNER(NORTH | EAST);

    private final int base;

    Piece(int base) {
      this.base = base;
    }
  }

  private final int width;

  private final int height;

  private final Piece[] pieces;

  private final int[] rotations;

  private final int[] solution;

  private final int sourceRow;

  private final int sinkRow;

  private CircuitBoard(int width, int height, Piece[] pieces, int[] rotations, int sourceRow,
      int sinkRow) {
    this.width = width;
    this.height = height;
    this.pieces = pieces;
    this.rotations = rotations;
    this.solution = rotations.clone();
    this.sourceRow = sourceRow;
    this.sinkRow = sinkRow;
  }

  // A random path from the left edge to the right edge decides the pieces on it; the other
  // cells get random pieces; then every rotation is scrambled (never left solved).
  public static CircuitBoard generate(int width, int height, Random random) {
    List<int[]> path = path(width, height, random);
    Piece[] pieces = new Piece[width * height];
    int[] rotations = new int[width * height];

    for (int i = 0; i < pieces.length; i++) {
      pieces[i] = random.nextBoolean() ? Piece.STRAIGHT : Piece.CORNER;
    }
    for (int i = 0; i < path.size(); i++) {
      int[] cell = path.get(i);
      int in = i == 0 ? WEST : direction(cell, path.get(i - 1));
      int out = i == path.size() - 1 ? EAST : direction(cell, path.get(i + 1));
      boolean straight = (in | out) == (NORTH | SOUTH) || (in | out) == (EAST | WEST);
      Piece piece = straight ? Piece.STRAIGHT : Piece.CORNER;

      pieces[cell[1] * width + cell[0]] = piece;
      rotations[cell[1] * width + cell[0]] = rotationFor(piece, in | out);
    }

    CircuitBoard board = new CircuitBoard(width, height, pieces, rotations.clone(),
        path.get(0)[1], path.get(path.size() - 1)[1]);

    do {
      for (int i = 0; i < pieces.length; i++) {
        board.rotations[i] = random.nextInt(4);
      }
    } while (board.isSolved());

    return board;
  }

  private static List<int[]> path(int width, int height, Random random) {
    while (true) {
      List<int[]> path = new ArrayList<>();
      Set<Integer> seen = new HashSet<>();
      int[] cell = {0, random.nextInt(height)};

      path.add(cell);
      seen.add(cell[1] * width + cell[0]);
      while (cell[0] < width - 1) {
        List<int[]> steps = new ArrayList<>();

        for (int[] step : new int[][] {{1, 0}, {0, 1}, {0, -1}}) {
          int x = cell[0] + step[0];
          int y = cell[1] + step[1];

          if (y >= 0 && y < height && !seen.contains(y * width + x)) {
            steps.add(new int[] {x, y});
          }
        }
        if (steps.isEmpty()) {
          break;
        }
        Collections.shuffle(steps, random);
        cell = steps.get(0);
        path.add(cell);
        seen.add(cell[1] * width + cell[0]);
      }
      if (cell[0] == width - 1 && path.size() > width) {
        return path;
      }
    }
  }

  private static int direction(int[] from, int[] to) {
    if (to[0] > from[0]) {
      return EAST;
    }
    if (to[0] < from[0]) {
      return WEST;
    }

    return to[1] > from[1] ? NORTH : SOUTH;
  }

  private static int rotationFor(Piece piece, int mask) {
    for (int rotation = 0; rotation < 4; rotation++) {
      if (turn(piece.base, rotation) == mask) {
        return rotation;
      }
    }

    throw new IllegalStateException("No rotation of " + piece + " fits " + mask);
  }

  static int turn(int mask, int times) {
    int result = mask;

    for (int i = 0; i < times; i++) {
      result = ((result << 1) | (result >> 3)) & 0xF;
    }

    return result;
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public int sourceRow() {
    return sourceRow;
  }

  public int sinkRow() {
    return sinkRow;
  }

  public Piece piece(int x, int y) {
    return pieces[y * width + x];
  }

  public int rotation(int x, int y) {
    return rotations[y * width + x];
  }

  public int connections(int x, int y) {
    return turn(piece(x, y).base, rotation(x, y));
  }

  public int solutionRotation(int x, int y) {
    return solution[y * width + x];
  }

  public void rotate(int x, int y) {
    rotations[y * width + x] = (rotations[y * width + x] + 1) % 4;
  }

  // The cells the beam passes through, in order, from the source.
  public List<int[]> powered() {
    List<int[]> cells = new ArrayList<>();
    Set<Integer> seen = new HashSet<>();
    int x = 0;
    int y = sourceRow;
    int from = WEST;

    while (x >= 0 && y >= 0 && x < width && y < height && seen.add(y * width + x)) {
      int mask = connections(x, y);

      if ((mask & from) == 0) {
        break;
      }
      cells.add(new int[] {x, y});

      int out = mask & ~from;

      if (out == EAST && x == width - 1 && y == sinkRow) {
        cells.add(new int[] {width, y});
        break;
      }
      switch (out) {
        case NORTH -> {
          y++;
          from = SOUTH;
        }
        case SOUTH -> {
          y--;
          from = NORTH;
        }
        case EAST -> {
          x++;
          from = WEST;
        }
        default -> {
          x--;
          from = EAST;
        }
      }
    }

    return cells;
  }

  public boolean isSolved() {
    List<int[]> cells = powered();

    return !cells.isEmpty() && cells.get(cells.size() - 1)[0] == width;
  }
}
