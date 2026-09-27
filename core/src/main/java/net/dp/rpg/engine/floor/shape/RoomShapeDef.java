package net.dp.rpg.engine.floor.shape;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class RoomShapeDef {

  public static final String NO_GROUP = "";

  private final String id;

  private final String group;

  private final RoomShape base;

  private final double holeRoomChance;

  private final List<ShapeVariant> variants;

  private RoomShapeDef(String id, String group, double holeRoomChance, RoomShape base) {
    if (holeRoomChance < 0.0 || holeRoomChance > 1.0) {
      throw new IllegalArgumentException("holeRoomChance must be within 0..1: " + id);
    }

    this.id = id;
    this.group = group;
    this.holeRoomChance = holeRoomChance;
    this.base = base;
    this.variants = expand();
  }

  public static RoomShapeDef of(String id, String... rows) {
    return new RoomShapeDef(id, NO_GROUP, 0.0, RoomShape.parse(rows));
  }

  public static RoomShapeDef grouped(String id, String group, String... rows) {
    return new RoomShapeDef(id, group, 0.0, RoomShape.parse(rows));
  }

  public static RoomShapeDef withHole(String id, String group, double holeRoomChance, String... rows) {
    return new RoomShapeDef(id, group, holeRoomChance, RoomShape.parse(rows));
  }

  public String id() {
    return id;
  }

  public String group() {
    return group;
  }

  public boolean grouped() {
    return !group.isEmpty();
  }

  public RoomShape base() {
    return base;
  }

  public double holeRoomChance() {
    return holeRoomChance;
  }

  public int size() {
    return base.size();
  }

  public List<ShapeVariant> variants() {
    return variants;
  }

  public ShapeVariant firstVariant() {
    return variants.get(0);
  }

  @Override
  public String toString() {
    return "%s(%d cells, %d rotations)".formatted(id, base.size(), variants.size());
  }

  private List<ShapeVariant> expand() {
    Map<String, RoomShape> unique = new LinkedHashMap<>();
    RoomShape rotated = base;

    for (int turn = 0; turn < 4; turn++) {
      unique.putIfAbsent(canonicalKey(rotated), rotated);
      rotated = rotate(rotated);
    }

    List<ShapeVariant> expanded = new ArrayList<>();

    unique.values().forEach(shape -> expanded.add(new ShapeVariant(this, shape, ShapeHoles.find(shape))));

    return List.copyOf(expanded);
  }

  private static RoomShape rotate(RoomShape shape) {
    List<RoomCell> cells = new ArrayList<>();

    shape.cells().forEach(cell -> cells.add(new RoomCell(-cell.y(), cell.x())));

    return RoomShape.of(cells);
  }

  private static String canonicalKey(RoomShape shape) {
    List<String> parts = new ArrayList<>();

    shape.cells().stream()
        .sorted(Comparator.comparingInt(RoomCell::y).thenComparingInt(RoomCell::x))
        .forEach(cell -> parts.add(cell.x() + ":" + cell.y()));

    return String.join(",", parts);
  }
}
