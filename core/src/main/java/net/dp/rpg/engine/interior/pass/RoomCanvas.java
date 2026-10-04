package net.dp.rpg.engine.interior.pass;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.corpus.RoomShapes;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.corpus.ZoneMap;
import net.dp.rpg.engine.interior.model.DoorPalette;
import net.dp.rpg.engine.interior.model.InteriorModel;
import net.dp.rpg.engine.interior.model.TileClasses;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.wfc.WeightTable;
import net.dp.rpg.engine.wfc.WfcGrid;
import net.dp.rpg.engine.wfc.WfcState;


public final class RoomCanvas {

  private final RoomBlueprint blueprint;

  private final InteriorModel model;

  private final ZoneMap zones;

  private final WfcGrid ground;

  private final int[] details;

  private final boolean[] reserved;

  private final List<TileMapObject> objects = new ArrayList<>();

  private RoomCanvas(RoomBlueprint blueprint, InteriorModel model, ZoneMap zones, WfcGrid ground) {
    this.blueprint = blueprint;
    this.model = model;
    this.zones = zones;
    this.ground = ground;
    this.details = new int[zones.width() * zones.height()];
    this.reserved = new boolean[zones.width() * zones.height()];
  }

  public static RoomCanvas of(RoomBlueprint blueprint, InteriorModel model, DoorPalette doors) {
    ZoneMap zones = ZoneMap.of(blueprint.shape(), blueprint::hasDoor);
    WfcGrid ground = new WfcGrid(zones.width(), zones.height(), model.rules(),
        weightTables(blueprint, model));

    for (int y = 0; y < zones.height(); y++) {
      for (int x = 0; x < zones.width(); x++) {
        Zone zone = zones.zoneAt(x, y);

        if (zone != Zone.VOID) {
          ground.setTable(x, y, zoneTableIndex(zone));
        }
      }
    }

    fixDoors(blueprint, doors, ground);

    return new RoomCanvas(blueprint, model, zones, ground);
  }

  public RoomBlueprint blueprint() {
    return blueprint;
  }

  public InteriorModel model() {
    return model;
  }

  public int width() {
    return zones.width();
  }

  public int height() {
    return zones.height();
  }

  public ZoneMap zones() {
    return zones;
  }

  public boolean isVoid(int x, int y) {
    return zones.zoneAt(x, y) == Zone.VOID;
  }

  public WfcGrid ground() {
    return ground;
  }

  public void setDetail(int x, int y, int runtimeTileId) {
    requireInsideRoom(x, y);
    details[x + y * zones.width()] = runtimeTileId + 1;
  }

  public int detailAt(int x, int y) {
    requireInsideRoom(x, y);

    return details[x + y * zones.width()] - 1;
  }

  public void reserve(int x, int y) {
    requireInsideRoom(x, y);
    reserved[x + y * zones.width()] = true;
  }

  public boolean isReserved(int x, int y) {
    requireInsideRoom(x, y);

    return reserved[x + y * zones.width()];
  }

  public void addObject(TileMapObject object) {
    objects.add(object);
  }

  public List<TileMapObject> objects() {
    return List.copyOf(objects);
  }

  public TileMapData toMapData(WfcState groundState, Random skinRandom) {
    int width = zones.width();
    int height = zones.height();
    TileClasses classes = model.classes();
    TileGrid groundGrid = new TileGrid(width, height);
    TileGrid detailsGrid = new TileGrid(width, height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (isVoid(x, y)) {
          continue;
        }

        int token = groundState.tokenAt(x, y);

        if (token < 0) {
          throw new IllegalStateException(
              "Tile %d,%d of room %s is not collapsed; the WFC attempt did not solve"
                  .formatted(x, y, blueprint.anchor()));
        }

        groundGrid.set(x, y, classes.skinOf(token, skinRandom));

        int detail = detailAt(x, y);

        if (detail != TileGrid.EMPTY) {
          detailsGrid.set(x, y, detail);
        }
      }
    }

    List<TileLayer> layers = List.of(
        new TileLayer("Ground", TileLayerKind.GROUND, groundGrid),
        new TileLayer("Details", TileLayerKind.DETAILS, detailsGrid));

    return new TileMapData(layers, renumbered(objects), mapProperties());
  }

  private static List<TileMapObject> renumbered(List<TileMapObject> source) {
    List<TileMapObject> numbered = new ArrayList<>(source.size());

    for (TileMapObject object : source) {
      numbered.add(new TileMapObject(numbered.size() + 1, object.name(), object.type(), object.x(),
          object.y(), object.width(), object.height(), object.properties()));
    }

    return numbered;
  }

  public Map<String, Object> mapProperties() {
    Map<String, Object> properties = new LinkedHashMap<>();

    properties.put(RoomShapes.SHAPE_PROPERTY, RoomShapes.encode(blueprint.shape()));
    properties.put("roomShape", blueprint.variant().id());
    properties.put("roomType", blueprint.type().name());
    properties.put("roomSeed", String.valueOf(blueprint.seed()));

    return properties;
  }

  private static void fixDoors(RoomBlueprint blueprint, DoorPalette doors, WfcGrid ground) {
    blueprint.doors().forEach((edge, doorType) ->
        ground.fix(edge.doorTileX(), edge.doorTileY(), doors.tokenOf(doorType)));
  }

  private static List<WeightTable> weightTables(RoomBlueprint blueprint, InteriorModel model) {
    List<WeightTable> tables = new ArrayList<>();

    tables.add(weightsFor(blueprint.type(), Zone.WALL, model));
    tables.add(weightsFor(blueprint.type(), Zone.INTERIOR, model));

    return tables;
  }

  private static WeightTable weightsFor(RoomType type, Zone zone, InteriorModel model) {
    return model.hasWeights(type, zone) ? model.weightsOf(type, zone)
        : model.weightsOf(RoomType.NORMAL, zone);
  }

  private static int zoneTableIndex(Zone zone) {
    return switch (zone) {
      case WALL, DOOR -> 0;
      case INTERIOR -> 1;
      case VOID -> throw new IllegalArgumentException("VOID has no weight table");
    };
  }

  private void requireInsideRoom(int x, int y) {
    if (!zones.isInside(x, y) || isVoid(x, y)) {
      throw new IllegalArgumentException("Tile %d,%d is outside the room or in its VOID hole".formatted(x, y));
    }
  }
}
