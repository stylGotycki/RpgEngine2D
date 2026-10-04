package net.dp.rpg.engine.interior.prefab;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.interior.corpus.CorpusIssue;
import net.dp.rpg.engine.interior.corpus.CorpusSample;
import net.dp.rpg.engine.interior.corpus.PrefabAnchor;
import net.dp.rpg.engine.interior.corpus.PrefabRegion;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.Direction;

public final class PrefabExtractor {

  public static final String ANCHOR_PROPERTY = "anchor";

  public static final String FLIPPABLE_PROPERTY = "flippable";

  public static final String WEIGHT_PROPERTY = "weight";

  private static final double DEFAULT_WEIGHT = 1.0;

  private PrefabExtractor() {
  }

  public static List<Prefab> extract(CorpusSample sample, List<CorpusIssue> issues) {
    List<Prefab> prefabs = new ArrayList<>();

    for (PrefabRegion region : sample.prefabs()) {
      prefabs.add(extractOne(sample, region, issues));
    }

    return prefabs;
  }

  private static Prefab extractOne(CorpusSample sample, PrefabRegion region,
                                   List<CorpusIssue> issues) {
    TileGrid ground = cut(sample.ground().grid(), region);
    TileGrid details = sample.details().map(TileLayer::grid).map(grid -> cut(grid, region))
        .orElse(new TileGrid(region.width(), region.height()));
    AnchorAndFacing anchor = readAnchor(sample, region, issues);
    boolean flippable = readFlippable(region);
    double weight = readWeight(sample, region, issues);
    List<TileMapObject> localObjects = translate(region);

    return new Prefab(region.group(), sample.source(), region.width(), region.height(), ground,
        details, anchor.anchor(), anchor.facing(), flippable, weight, localObjects);
  }

  private record AnchorAndFacing(PrefabAnchor anchor, Direction facing) {
  }

  private static TileGrid cut(TileGrid source, PrefabRegion region) {
    TileGrid cut = new TileGrid(region.width(), region.height());

    for (int y = 0; y < region.height(); y++) {
      for (int x = 0; x < region.width(); x++) {
        cut.set(x, y, source.get(region.x() + x, region.y() + y));
      }
    }

    return cut;
  }

  private static List<TileMapObject> translate(PrefabRegion region) {
    return region.objects().stream()
        .map(object -> new TileMapObject(object.id(), object.name(), object.type(),
            object.x() - region.x(), object.y() - region.y(), object.width(), object.height(),
            object.properties()))
        .toList();
  }

  private static AnchorAndFacing readAnchor(CorpusSample sample, PrefabRegion region,
                                            List<CorpusIssue> issues) {
    Object declared = region.properties().get(ANCHOR_PROPERTY);
    Direction wallSide = wallSideOf(sample, region);

    if (declared instanceof String text && !text.isBlank()) {
      PrefabAnchor anchor;

      try {
        anchor = PrefabAnchor.of(text);
      } catch (IllegalArgumentException exception) {
        issues.add(CorpusIssue.warningAt(sample.source(), region.x(), region.y(),
            "PREFAB '%s': %s; defaulting to ANY".formatted(region.group(),
                exception.getMessage())));

        return new AnchorAndFacing(PrefabAnchor.ANY, null);
      }

      if (anchor == PrefabAnchor.WALL && wallSide == null) {
        throw new IllegalArgumentException(
            "PREFAB '%s' declares anchor WALL but its region touches zero or more than one wall "
                + "side in its sample; it must touch exactly one".formatted(region.group()));
      }

      return new AnchorAndFacing(anchor, anchor == PrefabAnchor.WALL ? wallSide : null);
    }

    return wallSide != null ? new AnchorAndFacing(PrefabAnchor.WALL, wallSide)
        : new AnchorAndFacing(PrefabAnchor.ANY, null);
  }

  private static Direction wallSideOf(CorpusSample sample, PrefabRegion region) {
    Direction found = null;

    for (Direction direction : Direction.values()) {
      if (touchesWall(sample, region, direction)) {
        if (found != null) {
          return null;
        }

        found = direction.opposite();
      }
    }

    return found;
  }

  private static boolean touchesWall(CorpusSample sample, PrefabRegion region,
                                     Direction direction) {
    boolean vertical = direction == Direction.NORTH || direction == Direction.SOUTH;
    int fixed = switch (direction) {
      case NORTH -> region.y() - 1;
      case SOUTH -> region.y() + region.height();
      case WEST -> region.x() - 1;
      case EAST -> region.x() + region.width();
    };
    int alongStart = vertical ? region.x() : region.y();
    int alongEnd = vertical ? region.x() + region.width() : region.y() + region.height();

    for (int along = alongStart; along < alongEnd; along++) {
      int x = vertical ? along : fixed;
      int y = vertical ? fixed : along;
      boolean inside = x >= 0 && x < sample.width() && y >= 0 && y < sample.height();

      if (inside && sample.zoneAt(x, y) == Zone.WALL) {
        return true;
      }
    }

    return false;
  }

  private static boolean readFlippable(PrefabRegion region) {
    Object declared = region.properties().get(FLIPPABLE_PROPERTY);

    return !Boolean.FALSE.equals(declared);
  }

  private static double readWeight(CorpusSample sample, PrefabRegion region,
                                   List<CorpusIssue> issues) {
    Object declared = region.properties().get(WEIGHT_PROPERTY);

    if (declared == null) {
      return DEFAULT_WEIGHT;
    }

    if (declared instanceof Number number && number.doubleValue() > 0.0) {
      return number.doubleValue();
    }

    issues.add(CorpusIssue.warningAt(sample.source(), region.x(), region.y(),
        "PREFAB '%s' has a non-positive weight '%s'; defaulting to %.1f".formatted(region.group(),
            declared, DEFAULT_WEIGHT)));

    return DEFAULT_WEIGHT;
  }
}
