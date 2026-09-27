package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class RoomTypes {

  private final Map<RoomType, RoomTypeDefinition> byType = new EnumMap<>(RoomType.class);

  private final ShapePool defaultShapes;

  private RoomTypes(List<RoomTypeDefinition> catalog, ShapePool defaultShapes) {
    this.defaultShapes = defaultShapes;

    catalog.forEach(definition -> byType.put(definition.type(), definition));
  }

  public static RoomTypes of(List<RoomTypeDefinition> catalog, ShapePool defaultShapes) {
    if (catalog.stream().noneMatch(RoomTypeDefinition::isFiller)) {
      throw new IllegalArgumentException("A catalog needs at least one filler type");
    }

    return new RoomTypes(catalog, defaultShapes);
  }

  public static RoomTypes defaults() {
    return of(DefaultRoomTypes.catalog(), ShapePools.STANDARD);
  }

  public RoomTypeDefinition definition(RoomType type) {
    RoomTypeDefinition definition = byType.get(type);

    if (definition == null) {
      throw new IllegalArgumentException("No definition for room type " + type);
    }

    return definition;
  }

  public ShapePool poolFor(RoomType type) {
    return definition(type).shapesOr(defaultShapes);
  }

  public ShapePool defaultShapes() {
    return defaultShapes;
  }

  public List<RoomTypeDefinition> all() {
    return List.copyOf(byType.values());
  }

  public List<RoomTypeDefinition> fillers() {
    List<RoomTypeDefinition> fillers = new ArrayList<>();

    byType.values().stream().filter(RoomTypeDefinition::isFiller).forEach(fillers::add);

    return fillers;
  }
}
