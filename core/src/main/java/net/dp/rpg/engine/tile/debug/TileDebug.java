package net.dp.rpg.engine.tile.debug;

import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileRole;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetManager;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.function.IntFunction;

public final class TileDebug {

  private static final String NEW_LINE = System.lineSeparator();

  private final TileTypeRegistry typeRegistry;

  private final TilesetManager tilesetManager;

  public TileDebug(TileTypeRegistry typeRegistry, TilesetManager tilesetManager) {
    if (typeRegistry == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    if (tilesetManager == null) {
      throw new IllegalArgumentException("Tileset manager must not be null");
    }

    this.typeRegistry = typeRegistry;
    this.tilesetManager = tilesetManager;
  }

  public String describeCatalog() {
    StringBuilder out = new StringBuilder(1024);

    out.append("Tile catalog: ").append(typeRegistry.size()).append(" types, ")
        .append(tilesetManager.size()).append(" tileset(s), active: ")
        .append(tilesetManager.getActiveTilesetId()).append(NEW_LINE);

    for (TilesetDefinition tileset : tilesetManager.getLoaded()) {
      out.append("  ").append(describeTileset(tileset)).append(NEW_LINE);

      List<String> uncovered = tileset.binding().findMissing(typeRegistry.ids());

      if (!uncovered.isEmpty()) {
        out.append("    cannot draw ").append(uncovered.size()).append(": ")
            .append(uncovered).append(NEW_LINE);
      }
    }

    for (TileRole role : TileRole.values()) {
      List<TileType> types = typeRegistry.findByRole(role);

      if (!types.isEmpty()) {
        out.append("  %-11s %2d  %s".formatted(role, types.size(),
            types.stream().map(TileType::id).toList())).append(NEW_LINE);
      }
    }

    return out.toString();
  }

  private String describeTileset(TilesetDefinition tileset) {
    int variants = tileset.binding().localIdsByTypeId().values().stream()
        .mapToInt(List::size)
        .sum();

    return "tileset '%s': %d tiles %dx%d in %d columns, %d types, %d variants, image %s"
        .formatted(tileset.id(), tileset.tileCount(), tileset.tileWidth(), tileset.tileHeight(),
            tileset.columns(), tileset.binding().size(), variants, tileset.imagePath());
  }

  public String describeMap(TileMapData map, String label) {
    StringBuilder out = new StringBuilder(512);

    out.append("Map '%s': %dx%d, %d layer(s), %d object(s), %d distinct types used"
        .formatted(label, map.width(), map.height(), map.layers().size(), map.objects().size(),
            map.usedRuntimeIds().size())).append(NEW_LINE);

    int cells = map.width() * map.height();

    for (TileLayer layer : map.layers()) {
      int filled = layer.grid().countNonEmpty();

      out.append("  layer '%s' (%s): %d/%d cells filled (%.1f%%)"
              .formatted(layer.name(), layer.kind(), filled, cells, 100.0 * filled / cells))
          .append(NEW_LINE);
    }

    for (TileMapObject object : map.objects()) {
      out.append("  object #%d %s '%s' at (%.1f, %.1f)%s"
              .formatted(object.id(), object.type(), object.name(), object.x(), object.y(),
                  object.isPoint() ? "" : " size %.1fx%.1f".formatted(object.width(), object.height())))
          .append(NEW_LINE);
    }

    return out.toString();
  }

  public String describeHistogram(TileMapData map) {
    Map<String, Integer> counts = new TreeMap<>();

    for (TileLayer layer : map.layers()) {
      TileGrid grid = layer.grid();

      for (int y = 0; y < grid.getHeight(); y++) {
        for (int x = 0; x < grid.getWidth(); x++) {
          int runtimeId = grid.get(x, y);

          if (runtimeId != TileGrid.EMPTY) {
            counts.merge(typeRegistry.require(runtimeId).id(), 1, Integer::sum);
          }
        }
      }
    }

    StringBuilder out = new StringBuilder(512);

    out.append("Tile usage:").append(NEW_LINE);

    counts.entrySet().stream()
        .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
        .forEach(entry -> out.append("  %5d  %s".formatted(entry.getValue(), entry.getKey()))
            .append(NEW_LINE));

    return out.toString();
  }

  public String dump(TileMapData map) {
    StringBuilder out = new StringBuilder(2048);

    for (TileLayer layer : map.layers()) {
      out.append("Layer '").append(layer.name()).append("' (").append(layer.kind()).append(")")
          .append(NEW_LINE)
          .append(dump(layer.grid()))
          .append(NEW_LINE);
    }

    return out.toString();
  }

  public String dump(TileGrid grid) {
    return grid.toDebugString(roleSymbols());
  }

  public String dumpWalkable(TileMapData map) {
    TileGrid merged = new TileGrid(map.width(), map.height());

    for (int y = 0; y < map.height(); y++) {
      for (int x = 0; x < map.width(); x++) {
        merged.set(x, y, map.topTileAt(x, y));
      }
    }

    return merged.toDebugString(
        runtimeId -> typeRegistry.require(runtimeId).walkable() ? " " : "#");
  }

  private IntFunction<String> roleSymbols() {
    return runtimeId -> switch (typeRegistry.require(runtimeId).role()) {
      case FLOOR -> "-";
      case WALL -> "#";
      case DOOR -> "+";
      case PIT -> "~";
      case HAZARD -> "^";
      case OBSTACLE -> "o";
      case DECORATION -> "*";
      case UNSPECIFIED -> "?";
    };
  }
}
