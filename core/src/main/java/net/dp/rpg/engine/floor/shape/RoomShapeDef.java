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

  private final List<ShapeVariant> variants;

  private RoomShapeDef(String id, String group, RoomShape base) {
    this.id = id;
    this.group = group;
    this.base = base;
    this.variants = expand();
  }

  public static RoomShapeDef of(String id, String... rows) {
    return new RoomShapeDef(id, NO_GROUP, RoomShape.parse(rows));
  }

  public static RoomShapeDef grouped(String id, String group, String... rows) {
    return new RoomShapeDef(id, group, RoomShape.parse(rows));
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
