package net.dp.rpg.engine.interior.corpus;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import net.dp.rpg.engine.floor.Shapes;
import net.dp.rpg.engine.floor.shape.RoomShapeDef;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileRole;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.InvalidRoomShapeException;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class CorpusScanner {

  public static final String ROOM_TYPE_PROPERTY = "roomType";

  public static final String ROOM_SHAPE_PROPERTY = "roomShape";

  public static final String PREFAB_TYPE = "PREFAB";

  public static final String ACCESS_TYPE = "ACCESS";

  public static final String PREFAB_GROUP_PROPERTY = "prefab";

  private static final float GRID_TOLERANCE = 1e-3f;

  private final TileTypeRegistry types;

  public CorpusScanner(TileTypeRegistry types) {
    if (types == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    this.types = types;
  }

  public ScanResult scan(String source, TileMapData map) {
    return new Scan(source, map).run();
  }

  /** The sample, or null when the map had errors, plus every issue found. */
  public record ScanResult(CorpusSample sample, List<CorpusIssue> issues) {

    public ScanResult {
      issues = List.copyOf(issues);
    }

    public boolean accepted() {
      return sample != null;
    }
  }

  private final class Scan {

    private final String source;

    private final TileMapData map;

    private final List<CorpusIssue> issues = new ArrayList<>();

    private Scan(String source, TileMapData map) {
      this.source = source;
      this.map = map;
    }

    private ScanResult run() {
      Optional<TileLayer> groundLayer = map.findLayer(TileLayerKind.GROUND);

      if (groundLayer.isEmpty()) {
        issues.add(CorpusIssue.error(source, "Map has no GROUND layer"));

        return reject();
      }

      RoomShape shape = readShape();

      if (shape == null) {
        return reject();
      }

      TileGrid ground = groundLayer.get().grid();
      ZoneMap zones = ZoneMap.of(shape, edge -> isDoor(ground, edge));

      checkGround(ground, zones);
      checkLayers(zones);
      checkShapeHint(shape);

      Set<RoomType> roomTypes = readRoomTypes();
      List<PrefabRegion> prefabs = readPrefabs(zones);
      List<TileMapObject> markers = readMarkers(zones, prefabs);

      if (issues.stream().anyMatch(CorpusIssue::isError)) {
        return reject();
      }

      CorpusSample sample =
          new CorpusSample(source, map, shape, roomTypes, zones, prefabs, markers);

      return new ScanResult(sample, issues);
    }

    private ScanResult reject() {
      return new ScanResult(null, issues);
    }

    private RoomShape readShape() {
      RoomShape shape;

      try {
        shape = RoomShapes.of(map);
      } catch (InvalidRoomShapeException exception) {
        issues.add(CorpusIssue.error(source, exception.getMessage()));

        return null;
      }

      if (shape.tileWidth() != map.width() || shape.tileHeight() != map.height()) {
        issues.add(CorpusIssue.error(source,
            "Shape %s needs a %dx%d map, but the map is %dx%d".formatted(
                RoomShapes.encode(shape), shape.tileWidth(), shape.tileHeight(), map.width(),
                map.height())));

        return null;
      }

      return shape;
    }

    private boolean isDoor(TileGrid ground, RoomEdge edge) {
      int id = ground.get(edge.doorTileX(), edge.doorTileY());

      return id != TileGrid.EMPTY && types.require(id).role() == TileRole.DOOR;
    }

    private void checkGround(TileGrid ground, ZoneMap zones) {
      for (int y = 0; y < zones.height(); y++) {
        for (int x = 0; x < zones.width(); x++) {
          Zone zone = zones.zoneAt(x, y);
          int id = ground.get(x, y);

          if (zone == Zone.VOID) {
            continue;
          }

          if (id == TileGrid.EMPTY) {
            issues.add(CorpusIssue.errorAt(source, x, y, "Ground tile is missing"));
            continue;
          }

          TileType type = types.require(id);

          if (zone == Zone.WALL && type.role() == TileRole.DOOR) {
            issues.add(CorpusIssue.errorAt(source, x, y,
                "Door %s is not in the middle of its wall".formatted(type.id())));
          } else if (zone == Zone.WALL && type.role() != TileRole.WALL) {
            issues.add(CorpusIssue.errorAt(source, x, y,
                "Wall band must hold a wall or a door, found %s".formatted(type.id())));
          } else if (zone == Zone.INTERIOR && type.role() == TileRole.DOOR) {
            issues.add(CorpusIssue.errorAt(source, x, y,
                "Door %s stands inside the room instead of in its wall".formatted(type.id())));
          }
        }
      }
    }

    private void checkLayers(ZoneMap zones) {
      Map<String, int[]> tally = new LinkedHashMap<>();

      for (TileLayer layer : map.layers()) {
        for (int y = 0; y < layer.height(); y++) {
          for (int x = 0; x < layer.width(); x++) {
            int id = layer.grid().get(x, y);

            if (id == TileGrid.EMPTY) {
              continue;
            }

            TileType type = types.require(id);
            Zone zone = zones.isInside(x, y) ? zones.zoneAt(x, y) : Zone.VOID;

            if (zone == Zone.VOID) {
              count(tally, "Layer '%s' has %%d tile(s) outside the room shape; they are ignored"
                  .formatted(layer.name()), x, y);
            } else if (layer.kind() == TileLayerKind.DETAILS && zone != Zone.INTERIOR) {
              count(tally, "Layer '%s' has %%d detail tile(s) on the wall band"
                  .formatted(layer.name()), x, y);
            }

            if (type.layer() != layer.kind()) {
              count(tally, "Tile %s belongs to %s but %%d of them sit in layer '%s'"
                  .formatted(type.id(), type.layer(), layer.name()), x, y);
            }
          }
        }
      }

      tally.forEach((message, entry) -> issues.add(
          CorpusIssue.warningAt(source, entry[1], entry[2], message.formatted(entry[0]))));
    }

    private void checkShapeHint(RoomShape shape) {
      Object hint = map.properties().get(ROOM_SHAPE_PROPERTY);

      if (!(hint instanceof String id) || id.isBlank()) {
        return;
      }

      Optional<RoomShapeDef> definition =
          Shapes.ALL.stream().filter(candidate -> candidate.id().equals(id)).findFirst();

      if (definition.isEmpty()) {
        issues.add(CorpusIssue.warning(source,
            "Unknown roomShape '%s'; known shapes are %s".formatted(id,
                Shapes.ALL.stream().map(RoomShapeDef::id).toList())));
      } else if (definition.get().variants().stream()
          .noneMatch(variant -> variant.shape().equals(shape))) {
        issues.add(CorpusIssue.warning(source,
            "roomShape '%s' does not match roomCells %s; roomCells wins".formatted(id,
                RoomShapes.encode(shape))));
      }
    }

    private Set<RoomType> readRoomTypes() {
      Object declared = map.properties().get(ROOM_TYPE_PROPERTY);

      if (!(declared instanceof String text) || text.isBlank()) {
        return EnumSet.allOf(RoomType.class);
      }

      Set<RoomType> roomTypes = EnumSet.noneOf(RoomType.class);

      for (String name : text.split(",")) {
        String trimmed = name.trim();

        try {
          roomTypes.add(RoomType.valueOf(trimmed));
        } catch (IllegalArgumentException exception) {
          issues.add(CorpusIssue.warning(source,
              "Unknown room type '%s' is ignored".formatted(trimmed)));
        }
      }

      if (roomTypes.isEmpty()) {
        issues.add(CorpusIssue.warning(source,
            "No known room type in '%s'; the sample applies to every type".formatted(text)));

        return EnumSet.allOf(RoomType.class);
      }

      return roomTypes;
    }

    private List<PrefabRegion> readPrefabs(ZoneMap zones) {
      List<PrefabRegion> regions = new ArrayList<>();

      for (TileMapObject object : map.findObjects(PREFAB_TYPE)) {
        PrefabRegion region = readPrefab(object, zones);

        if (region == null) {
          continue;
        }

        for (PrefabRegion other : regions) {
          if (region.overlaps(other)) {
            issues.add(CorpusIssue.errorAt(source, region.x(), region.y(),
                "PREFAB '%s' overlaps PREFAB '%s'".formatted(region.group(), other.group())));
          }
        }

        regions.add(region);
      }

      return regions;
    }

    private PrefabRegion readPrefab(TileMapObject object, ZoneMap zones) {
      int nearX = tileOf(object.x());
      int nearY = tileOf(object.y());

      if (object.isPoint()) {
        issues.add(CorpusIssue.errorAt(source, nearX, nearY,
            "PREFAB must be a rectangle, not a point"));

        return null;
      }

      if (!onGrid(object.x()) || !onGrid(object.y()) || !onGrid(object.width())
          || !onGrid(object.height())) {
        issues.add(CorpusIssue.errorAt(source, nearX, nearY,
            "PREFAB is not aligned to the tile grid"));

        return null;
      }

      Object group = object.properties().get(PREFAB_GROUP_PROPERTY);

      if (!(group instanceof String name) || name.isBlank()) {
        issues.add(CorpusIssue.errorAt(source, nearX, nearY,
            "PREFAB needs a '%s' property naming its group".formatted(PREFAB_GROUP_PROPERTY)));

        return null;
      }

      PrefabRegion region = new PrefabRegion(Math.round(object.x()), Math.round(object.y()),
          Math.round(object.width()), Math.round(object.height()), name, object.properties(),
          objectsWithin(object));

      for (int tileY = region.y(); tileY < region.y() + region.height(); tileY++) {
        for (int tileX = region.x(); tileX < region.x() + region.width(); tileX++) {
          if (!zones.isInside(tileX, tileY) || zones.zoneAt(tileX, tileY) != Zone.INTERIOR) {
            String message = "PREFAB '%s' must lie inside the room interior, off the wall band";

            issues.add(CorpusIssue.errorAt(source, tileX, tileY, message.formatted(name)));

            return null;
          }
        }
      }

      return region;
    }

    private List<TileMapObject> objectsWithin(TileMapObject prefab) {
      int left = Math.round(prefab.x());
      int top = Math.round(prefab.y());
      int right = left + Math.round(prefab.width());
      int bottom = top + Math.round(prefab.height());
      List<TileMapObject> inside = new ArrayList<>();

      for (TileMapObject candidate : map.objects()) {
        int x = tileOf(candidate.x());
        int y = tileOf(candidate.y());

        if (!candidate.isType(PREFAB_TYPE) && x >= left && x < right && y >= top && y < bottom) {
          inside.add(candidate);
        }
      }

      return inside;
    }

    private List<TileMapObject> readMarkers(ZoneMap zones, List<PrefabRegion> prefabs) {
      List<TileMapObject> markers = new ArrayList<>();

      for (TileMapObject object : map.objects()) {
        if (object.isType(PREFAB_TYPE)) {
          continue;
        }

        int x = tileOf(object.x());
        int y = tileOf(object.y());

        if (prefabs.stream().anyMatch(prefab -> prefab.contains(x, y))) {
          continue;
        }

        if (object.isType(ACCESS_TYPE)) {
          issues.add(CorpusIssue.warningAt(source, x, y, "ACCESS outside any PREFAB is ignored"));
        } else if (!zones.isInside(x, y) || !zones.zoneAt(x, y).isInsideRoom()) {
          issues.add(CorpusIssue.warningAt(source, x, y, "Marker %s lies outside the room shape and is ignored".formatted(object.type())));
        } else {
          markers.add(object);
        }
      }

      return markers;
    }

    private void count(Map<String, int[]> tally, String message, int x, int y) {
      tally.computeIfAbsent(message, key -> new int[] {0, x, y})[0]++;
    }

    private int tileOf(float coordinate) {
      return (int) Math.floor(coordinate);
    }

    private boolean onGrid(float coordinate) {
      return Math.abs(coordinate - Math.round(coordinate)) < GRID_TOLERANCE;
    }
  }
}
