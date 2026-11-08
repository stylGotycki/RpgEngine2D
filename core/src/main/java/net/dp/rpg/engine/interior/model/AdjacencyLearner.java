package net.dp.rpg.engine.interior.model;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.corpus.CorpusSample;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.corpus.ZoneMap;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.wfc.AdjacencyRules;
import net.dp.rpg.engine.wfc.WeightTable;

public final class AdjacencyLearner {

  private final TileClasses classes;

  public AdjacencyLearner(TileClasses classes) {
    if (classes == null) {
      throw new IllegalArgumentException("Tile classes must not be null");
    }

    this.classes = classes;
  }

  public InteriorModel learn(List<CorpusSample> samples) {
    if (samples.isEmpty()) {
      throw new IllegalArgumentException("Cannot learn an interior model from an empty corpus");
    }

    AdjacencyRules.Builder rules = AdjacencyRules.builder(classes.tokenCount());
    Map<RoomType, Map<Zone, ZoneStatistics>> statsByTypeAndZone = new EnumMap<>(RoomType.class);
    Map<Zone, ZoneStatistics> motifWide = freshZoneMap();

    for (CorpusSample sample : samples) {
      learnAdjacency(rules, sample);

      double weightPerType = 1.0 / sample.roomTypes().size();

      for (RoomType type : sample.roomTypes()) {
        Map<Zone, ZoneStatistics> byZone =
            statsByTypeAndZone.computeIfAbsent(type, ignored -> freshZoneMap());

        learnZoneCounts(sample, byZone, motifWide, weightPerType);
      }
    }

    return new InteriorModel(classes, rules.build(), toWeightTables(statsByTypeAndZone, motifWide));
  }

  private void learnAdjacency(AdjacencyRules.Builder rules, CorpusSample sample) {
    TileGrid grid = sample.ground().grid();

    for (int y = 0; y < grid.getHeight(); y++) {
      for (int x = 0; x < grid.getWidth(); x++) {
        int here = classes.classOf(grid.get(x, y));

        for (Direction direction : Direction.values()) {
          int neighbourX = x + direction.getDeltaX();
          int neighbourY = y + direction.getDeltaY();
          int there = grid.isInside(neighbourX, neighbourY)
              ? classes.classOf(grid.get(neighbourX, neighbourY))
              : TileClasses.VOID;

          rules.allow(here, direction, there);
        }
      }
    }
  }

  private void learnZoneCounts(CorpusSample sample, Map<Zone, ZoneStatistics> byZone,
                               Map<Zone, ZoneStatistics> motifWide, double weight) {
    ZoneMap zones = sample.zones();
    TileGrid grid = sample.ground().grid();

    for (int y = 0; y < grid.getHeight(); y++) {
      for (int x = 0; x < grid.getWidth(); x++) {
        Zone zone = zones.zoneAt(x, y);

        if (zone == Zone.VOID || sample.isInPrefab(x, y)) {
          continue;
        }

        int token = classes.classOf(grid.get(x, y));

        byZone.get(zone).add(token, weight);
        motifWide.get(zone).add(token, weight);
      }
    }
  }

  private Map<RoomType, Map<Zone, WeightTable>> toWeightTables(
      Map<RoomType, Map<Zone, ZoneStatistics>> statsByTypeAndZone,
      Map<Zone, ZoneStatistics> motifWide) {
    Map<RoomType, Map<Zone, WeightTable>> weights = new EnumMap<>(RoomType.class);

    statsByTypeAndZone.forEach((type, byZone) -> {
      Map<Zone, WeightTable> tables = new EnumMap<>(Zone.class);

      byZone.forEach((zone, stats) -> {
        if (stats.total() > 0.0) {
          tables.put(zone, stats.toWeightTable(motifWide.get(zone)));
        }
      });

      weights.put(type, tables);
    });

    return weights;
  }

  private Map<Zone, ZoneStatistics> freshZoneMap() {
    Map<Zone, ZoneStatistics> byZone = new EnumMap<>(Zone.class);

    for (Zone zone : Zone.values()) {
      if (zone != Zone.VOID) {
        byZone.put(zone, ZoneStatistics.zero(classes.tokenCount()));
      }
    }

    return byZone;
  }
}
