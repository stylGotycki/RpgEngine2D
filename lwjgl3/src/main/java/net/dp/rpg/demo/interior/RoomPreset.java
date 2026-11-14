package net.dp.rpg.demo.interior;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.Shapes;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.shape.RoomShapeDef;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

final class RoomPreset {

  static final List<RoomShapeDef> SHAPES = Shapes.ALL;

  static final List<RoomType> TYPES = List.of(RoomType.NORMAL, RoomType.SHOP,
      RoomType.POWER_FIELD, RoomType.VAULT, RoomType.BOSS);

  private RoomPreset() {
  }

  static RoomBlueprint blueprintOf(int shapeIndex, int typeIndex, long seed) {
    RoomShapeDef shapeDef = SHAPES.get(Math.floorMod(shapeIndex, SHAPES.size()));
    RoomType type = TYPES.get(Math.floorMod(typeIndex, TYPES.size()));
    ShapeVariant variant = shapeDef.firstVariant();
    DoorType doorType = type == RoomType.VAULT ? DoorType.LOCKED : DoorType.NORMAL;
    Map<RoomEdge, DoorType> doors = doorsFor(variant, doorType);

    return new RoomBlueprint(RoomCell.ORIGIN, variant, type, doors, null, 0, seed);
  }

  private static Map<RoomEdge, DoorType> doorsFor(ShapeVariant variant, DoorType doorType) {
    List<RoomEdge> outer = variant.shape().outerEdges();
    Map<RoomEdge, DoorType> doors = new LinkedHashMap<>();
    int step = outer.size() <= 4 ? 1 : 2;

    for (int index = 0; index < outer.size(); index += step) {
      doors.put(outer.get(index), doorType);
    }

    return doors;
  }
}
