package net.dp.rpg.engine.interior.pass;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.model.WalkableClasses;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.wfc.TokenSet;
import net.dp.rpg.engine.wfc.WfcGrid;


public final class WalkableSpinePass {

  public static final double DEFAULT_WANDER_WEIGHT = 3.0;

  private final WalkableClasses walkable;

  /** How strongly noise can outweigh a step's cost of 1; see {@link #DEFAULT_WANDER_WEIGHT}. */
  private final double wanderWeight;

  public WalkableSpinePass(WalkableClasses walkable, double wanderWeight) {
    if (walkable == null) {
      throw new IllegalArgumentException("Walkable classes must not be null");
    }

    this.walkable = walkable;
    this.wanderWeight = wanderWeight;
  }

  public boolean apply(RoomCanvas canvas, List<TileMapObject> extraPoints, Random random) {
    List<int[]> targets = SpineTargets.of(canvas.blueprint(), extraPoints);

    if (targets.size() < 2) {
      return true;
    }

    List<int[]> connected = new ArrayList<>();
    connected.add(targets.get(0));

    for (int i = 1; i < targets.size(); i++) {
      int[] target = targets.get(i);
      int[] nearest = nearestTo(target, connected);
      List<int[]> path = findPath(canvas, nearest, target, random);

      if (path == null) {
        return false;
      }

      restrict(canvas, path);
      connected.add(target);
    }

    return true;
  }

  private static int[] nearestTo(int[] target, List<int[]> connected) {
    int[] best = connected.get(0);
    int bestDistance = manhattan(target, best);

    for (int[] candidate : connected) {
      int distance = manhattan(target, candidate);

      if (distance < bestDistance) {
        bestDistance = distance;
        best = candidate;
      }
    }

    return best;
  }

  private List<int[]> findPath(RoomCanvas canvas, int[] from, int[] to, Random random) {
    int width = canvas.width();
    int height = canvas.height();
    double[] bestCost = new double[width * height];
    int[] cameFrom = new int[width * height];
    boolean[] closed = new boolean[width * height];

    java.util.Arrays.fill(bestCost, Double.POSITIVE_INFINITY);
    java.util.Arrays.fill(cameFrom, -1);

    PriorityQueue<long[]> open = new PriorityQueue<>((a, b) -> Double.compare(
        Double.longBitsToDouble(a[1]), Double.longBitsToDouble(b[1])));
    double[] noise = noiseFor(width, height, random);
    int start = from[0] + from[1] * width;
    int goal = to[0] + to[1] * width;

    bestCost[start] = 0.0;
    open.add(new long[] {start, Double.doubleToLongBits(heuristic(from, to))});

    while (!open.isEmpty()) {
      int cell = (int) open.poll()[0];

      if (cell == goal) {
        return reconstruct(cameFrom, cell, width);
      }

      if (closed[cell]) {
        continue;
      }

      closed[cell] = true;

      int x = cell % width;
      int y = cell / width;

      for (Direction direction : Direction.values()) {
        int neighbourX = x + direction.getDeltaX();
        int neighbourY = y + direction.getDeltaY();

        if (!canEnter(canvas, neighbourX, neighbourY, neighbourX == to[0] && neighbourY == to[1])) {
          continue;
        }

        int neighbour = neighbourX + neighbourY * width;
        double cost = bestCost[cell] + 1.0 + noise[neighbour] * wanderWeight;

        if (cost < bestCost[neighbour]) {
          bestCost[neighbour] = cost;
          cameFrom[neighbour] = cell;

          double priority = cost + heuristic(new int[] {neighbourX, neighbourY}, to);

          open.add(new long[] {neighbour, Double.doubleToLongBits(priority)});
        }
      }
    }

    return null;
  }

  private boolean canEnter(RoomCanvas canvas, int x, int y, boolean isTarget) {
    if (!canvas.zones().isInside(x, y) || canvas.isVoid(x, y)) {
      return false;
    }

    if (isTarget) {
      return true;
    }

    Zone zone = canvas.zones().zoneAt(x, y);

    if (zone != Zone.INTERIOR) {
      return false;
    }

    return !canvas.ground().isFixed(x, y)
        && !walkable.walkableSubsetOf(canvas.ground().startingDomain(x, y)).isEmpty();
  }

  private void restrict(RoomCanvas canvas, List<int[]> path) {
    for (int[] tile : path) {
      int x = tile[0];
      int y = tile[1];

      canvas.reserve(x, y);

      if (canvas.zones().zoneAt(x, y) != Zone.INTERIOR || canvas.ground().isFixed(x, y)) {
        continue;
      }

      TokenSet current = canvas.ground().startingDomain(x, y);
      TokenSet narrowed = walkable.walkableSubsetOf(current);

      if (narrowed.isEmpty() || !stillConsistentIfRestricted(canvas, x, y, narrowed)) {
        continue;
      }

      canvas.ground().restrict(x, y, narrowed);
    }
  }

  private boolean stillConsistentIfRestricted(RoomCanvas canvas, int x, int y, TokenSet narrowed) {
    for (Direction direction : Direction.values()) {
      int neighbourX = x + direction.getDeltaX();
      int neighbourY = y + direction.getDeltaY();

      boolean outside = !canvas.zones().isInside(neighbourX, neighbourY)
          || canvas.isVoid(neighbourX, neighbourY);

      if (outside) {
        continue;
      }

      TokenSet neighbourDomain = canvas.ground().startingDomain(neighbourX, neighbourY);
      boolean anyCompatible = false;

      for (int token : narrowed.toArray()) {
        if (!canvas.model().rules().neighbours(token, direction).intersection(neighbourDomain)
            .isEmpty()) {
          anyCompatible = true;
          break;
        }
      }

      if (!anyCompatible) {
        return false;
      }
    }

    return true;
  }

  private static List<int[]> reconstruct(int[] cameFrom, int cell, int width) {
    ArrayDeque<int[]> path = new ArrayDeque<>();

    while (cell != -1) {
      path.addFirst(new int[] {cell % width, cell / width});
      cell = cameFrom[cell];
    }

    return new ArrayList<>(path);
  }

  private static double heuristic(int[] a, int[] b) {
    return manhattan(a, b);
  }

  private static int manhattan(int[] a, int[] b) {
    return Math.abs(a[0] - b[0]) + Math.abs(a[1] - b[1]);
  }

  private static double[] noiseFor(int width, int height, Random random) {
    double[] noise = new double[width * height];

    for (int i = 0; i < noise.length; i++) {
      noise[i] = random.nextDouble();
    }

    return noise;
  }
}
