package net.dp.rpg.engine.interior.detail;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.dp.rpg.engine.interior.corpus.CorpusSample;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.model.TileClasses;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;


public final class DetailLearner {

  private DetailLearner() {
  }

  public static Map<Integer, DetailStatistics> learn(List<CorpusSample> samples,
      TileClasses groundClasses) {
    Map<Integer, long[]> tilesByGround = new LinkedHashMap<>();
    Map<Integer, Accumulator> byType = new LinkedHashMap<>();

    for (CorpusSample sample : samples) {
      Optional<TileLayer> detailsLayer = sample.details();

      if (detailsLayer.isEmpty()) {
        continue;
      }

      int[][] wallDistance = WallDistance.of(sample.zones());
      Map<Integer, List<int[]>> positionsInSample = new LinkedHashMap<>();

      for (int y = 0; y < sample.height(); y++) {
        for (int x = 0; x < sample.width(); x++) {
          if (sample.zoneAt(x, y) != Zone.INTERIOR || sample.isInPrefab(x, y)) {
            continue;
          }

          int groundClass = groundClasses.classOf(sample.ground().grid().get(x, y));
          int bucket = DetailStatistics.bucketOf(wallDistance[x][y]);
          int detailId = detailsLayer.get().grid().get(x, y);

          tilesByGround.computeIfAbsent(groundClass, ignored -> new long[buckets()])[bucket]++;

          if (detailId == TileGrid.EMPTY) {
            continue;
          }

          byType.computeIfAbsent(detailId, ignored -> new Accumulator()).record(groundClass, bucket);
          positionsInSample.computeIfAbsent(detailId, ignored -> new ArrayList<>())
              .add(new int[] {x, y});
        }
      }

      positionsInSample.forEach((detailId, positions) ->
          byType.get(detailId).recordSpacing(positions));
    }

    Map<Integer, DetailStatistics> statistics = new LinkedHashMap<>();

    byType.forEach((detailId, accumulator) ->
        statistics.put(detailId, accumulator.toStatistics(detailId, tilesByGround)));

    return statistics;
  }

  private static int buckets() {
    return DetailStatistics.DISTANCE_BUCKETS;
  }

  private static final class Accumulator {

    private final Map<Integer, long[]> occurrences = new LinkedHashMap<>();

    private int minSpacingSoFar = Integer.MAX_VALUE;

    void record(int groundClass, int bucket) {
      occurrences.computeIfAbsent(groundClass, ignored -> new long[buckets()])[bucket]++;
    }

    void recordSpacing(List<int[]> positionsInOneSample) {
      if (positionsInOneSample.size() < 2) {
        return;
      }

      int sampleMin = Integer.MAX_VALUE;

      for (int i = 0; i < positionsInOneSample.size(); i++) {
        for (int j = i + 1; j < positionsInOneSample.size(); j++) {
          int[] a = positionsInOneSample.get(i);
          int[] b = positionsInOneSample.get(j);
          int distance = Math.max(Math.abs(a[0] - b[0]), Math.abs(a[1] - b[1]));

          sampleMin = Math.min(sampleMin, distance);
        }
      }

      minSpacingSoFar = Math.min(minSpacingSoFar, sampleMin);
    }

    DetailStatistics toStatistics(int detailId, Map<Integer, long[]> tilesByGround) {
      Map<Integer, double[]> chanceByGround = new LinkedHashMap<>();

      occurrences.forEach((groundClass, counts) -> {
        long[] tiles = tilesByGround.get(groundClass);
        double[] chances = new double[buckets()];

        for (int bucket = 0; bucket < chances.length; bucket++) {
          chances[bucket] = tiles[bucket] > 0 ? (double) counts[bucket] / tiles[bucket] : 0.0;
        }

        chanceByGround.put(groundClass, chances);
      });

      int minSpacing = minSpacingSoFar == Integer.MAX_VALUE ? 0 : minSpacingSoFar;

      return new DetailStatistics(detailId, chanceByGround, minSpacing);
    }
  }
}
