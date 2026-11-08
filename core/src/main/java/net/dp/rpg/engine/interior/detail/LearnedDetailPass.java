package net.dp.rpg.engine.interior.detail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.IntPredicate;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.wfc.WfcState;


public final class LearnedDetailPass {

  private final Map<Integer, DetailStatistics> statistics;

  private final IntPredicate blocksWalking;

  public LearnedDetailPass(Map<Integer, DetailStatistics> statistics,
      IntPredicate blocksWalking) {
    if (statistics == null || blocksWalking == null) {
      throw new IllegalArgumentException("Statistics and walkability test must not be null");
    }

    this.statistics = statistics;
    this.blocksWalking = blocksWalking;
  }

  public void apply(RoomCanvas canvas, WfcState groundState, Random random) {
    int[][] wallDistance = WallDistance.of(canvas.zones());

    for (DetailStatistics detail : statistics.values()) {
      scatterOne(canvas, groundState, wallDistance, detail, random);
    }
  }

  private void scatterOne(RoomCanvas canvas, WfcState groundState, int[][] wallDistance,
      DetailStatistics detail, Random random) {
    List<int[]> candidates = candidatesFor(canvas, detail);
    List<int[]> placed = new ArrayList<>();

    Collections.shuffle(candidates, random);

    for (int[] tile : candidates) {
      int x = tile[0];
      int y = tile[1];

      if (tooClose(placed, x, y, detail.minSpacing())) {
        continue;
      }

      double chance = detail.chanceAt(groundState.tokenAt(x, y), wallDistance[x][y]);

      if (random.nextDouble() < chance) {
        canvas.setDetail(x, y, detail.runtimeId());
        placed.add(tile);
      }
    }
  }

  private List<int[]> candidatesFor(RoomCanvas canvas, DetailStatistics detail) {
    boolean blocking = blocksWalking.test(detail.runtimeId());
    List<int[]> candidates = new ArrayList<>();

    for (int y = 0; y < canvas.height(); y++) {
      for (int x = 0; x < canvas.width(); x++) {
        boolean free = canvas.zones().zoneAt(x, y) == Zone.INTERIOR
            && !canvas.ground().isFixed(x, y)
            && canvas.detailAt(x, y) == TileGrid.EMPTY;

        if (free && !(blocking && canvas.isReserved(x, y))) {
          candidates.add(new int[] {x, y});
        }
      }
    }

    return candidates;
  }

  private static boolean tooClose(List<int[]> placed, int x, int y, int minSpacing) {
    if (minSpacing <= 0) {
      return false;
    }

    for (int[] other : placed) {
      if (Math.max(Math.abs(other[0] - x), Math.abs(other[1] - y)) < minSpacing) {
        return true;
      }
    }

    return false;
  }
}
