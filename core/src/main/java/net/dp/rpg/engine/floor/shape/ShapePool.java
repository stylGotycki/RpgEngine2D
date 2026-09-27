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

  public record ShapeEntry(RoomShapeDef shape, double weight) {

    public ShapeEntry {
      if (!Double.isFinite(weight) || weight <= 0.0) {
        throw new IllegalArgumentException("Shape weight must be greater than zero: " + shape.id());
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
      entries.add(new ShapeEntry(shape, weight));

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
