package net.dp.rpg.engine.interior.detail;

import java.util.ArrayDeque;
import java.util.Arrays;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.corpus.ZoneMap;

final class WallDistance {

  private static final int[] DX = {-1, 0, 1, -1, 1, -1, 0, 1};

  private static final int[] DY = {-1, -1, -1, 0, 0, 1, 1, 1};

  private WallDistance() {
  }

  static int[][] of(ZoneMap zones) {
    int width = zones.width();
    int height = zones.height();
    int[][] distance = new int[width][height];
    ArrayDeque<int[]> queue = new ArrayDeque<>();

    for (int[] row : distance) {
      Arrays.fill(row, -1);
    }

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (zones.zoneAt(x, y) == Zone.WALL) {
          distance[x][y] = 0;
          queue.add(new int[] {x, y});
        }
      }
    }

    while (!queue.isEmpty()) {
      int[] cell = queue.poll();

      for (int i = 0; i < DX.length; i++) {
        int nx = cell[0] + DX[i];
        int ny = cell[1] + DY[i];

        if (nx < 0 || nx >= width || ny < 0 || ny >= height || distance[nx][ny] != -1) {
          continue;
        }

        distance[nx][ny] = distance[cell[0]][cell[1]] + 1;
        queue.add(new int[] {nx, ny});
      }
    }

    return distance;
  }
}
