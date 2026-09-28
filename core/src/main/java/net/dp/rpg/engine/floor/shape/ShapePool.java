package net.dp.rpg.engine.floor.shape;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public final class ShapePool {

  private final String id;

  private final List<ShapeEntry> entries;

  private ShapePool(String id, List<ShapeEntry> entries) {
    this.id = id;
    this.entries = List.copyOf(entries);
  }

  public static Builder named(String id) {
    return new Builder(id);
  }

  public String id() {
    return id;
  }

  public List<ShapeEntry> entries() {
    return entries;
  }

  public double holeRoomChanceOf(RoomShapeDef shape) {
    return entries.stream()
        .filter(entry -> entry.shape() == shape)
        .mapToDouble(ShapeEntry::holeRoomChance)
        .findFirst()
        .orElse(0.0);
  }

  public boolean contains(RoomShapeDef shape) {
    return entries.stream().anyMatch(entry -> entry.shape() == shape);
  }

  public ShapeEntry smallest() {
    return entries.stream()
        .min((first, second) -> Integer.compare(first.shape().size(), second.shape().size()))
        .orElseThrow();
  }

  public ShapePool restrictedTo(Collection<String> allowedShapeIds) {
    List<ShapeEntry> kept = entries.stream()
        .filter(entry -> allowedShapeIds.contains(entry.shape().id()))
        .toList();

    if (kept.isEmpty()) {
      throw new IllegalArgumentException("Pool %s has no shape left after restriction".formatted(id));
    }

    return new ShapePool(id, kept);
  }

  @Override
  public String toString() {
    return "pool %s(%d shapes)".formatted(id, entries.size());
  }

  public record ShapeEntry(RoomShapeDef shape, double weight, double holeRoomChance) {

    public ShapeEntry {
      if (!Double.isFinite(weight) || weight <= 0.0) {
        throw new IllegalArgumentException("Shape weight must be greater than zero: " + shape.id());
      }

      if (holeRoomChance < 0.0 || holeRoomChance > 1.0) {
        throw new IllegalArgumentException("holeRoomChance must be within 0..1: " + shape.id());
      }
    }
  }

  public static final class Builder {

    private final String id;

    private final List<ShapeEntry> entries = new ArrayList<>();

    private Builder(String id) {
      this.id = id;
    }

    public Builder with(RoomShapeDef shape, double weight) {
      return with(shape, weight, 0.0);
    }

    public Builder with(RoomShapeDef shape, double weight, double holeRoomChance) {
      entries.add(new ShapeEntry(shape, weight, holeRoomChance));

      return this;
    }

    public ShapePool build() {
      if (entries.isEmpty()) {
        throw new IllegalArgumentException("Shape pool must not be empty: " + id);
      }

      return new ShapePool(id, entries);
    }
  }
}
