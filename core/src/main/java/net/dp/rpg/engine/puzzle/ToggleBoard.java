package net.dp.rpg.engine.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Plates that flip between off and on whenever the player steps onto them; all on solves it.
// The generator walks a random path from the entry and sets the start so that exactly this
// path lights every plate, so a solution always exists.
public final class ToggleBoard {

  private final int width;

  private final int height;

  private final boolean[] initial;

  private final boolean[] on;

  private final List<int[]> solution;

  private ToggleBoard(int width, int height, boolean[] initial, List<int[]> solution) {
    this.width = width;
    this.height = height;
    this.initial = initial.clone();
    this.on = initial.clone();
    this.solution = List.copyOf(solution);
  }

  public static ToggleBoard generate(int width, int height, Random random) {
    while (true) {
      List<int[]> path = new ArrayList<>();
      int[] cell = {random.nextInt(width), 0};
      int length = width * height + random.nextInt(width * height / 2 + 1);

      path.add(cell);
      for (int step = 1; step < length; step++) {
        List<int[]> next = new ArrayList<>();

        for (int[] move : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
          int x = cell[0] + move[0];
          int y = cell[1] + move[1];
          boolean back = path.size() > 1 && path.get(path.size() - 2)[0] == x
              && path.get(path.size() - 2)[1] == y;

          if (x >= 0 && y >= 0 && x < width && y < height && !back) {
            next.add(new int[] {x, y});
          }
        }
        cell = next.get(random.nextInt(next.size()));
        path.add(cell);
      }

      boolean[] start = new boolean[width * height];
      java.util.Arrays.fill(start, true);
      for (int[] visit : path) {
        start[visit[1] * width + visit[0]] ^= true;
      }

      int off = 0;
      for (boolean plate : start) {
        off += plate ? 0 : 1;
      }
      // Too easy when almost everything is lit already.
      if (off >= width * height / 2) {
        return new ToggleBoard(width, height, start, path);
      }
    }
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public boolean isOn(int x, int y) {
    return on[y * width + x];
  }

  public void step(int x, int y) {
    if (!isSolved()) {
      on[y * width + x] ^= true;
    }
  }

  public void reset() {
    System.arraycopy(initial, 0, on, 0, on.length);
  }

  public boolean isSolved() {
    for (boolean plate : on) {
      if (!plate) {
        return false;
      }
    }

    return true;
  }

  public List<int[]> solution() {
    return solution;
  }
}
