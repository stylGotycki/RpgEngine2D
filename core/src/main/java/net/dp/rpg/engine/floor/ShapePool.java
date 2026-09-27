package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
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

  public ShapeEntry smallest() {
    return entries.stream()
        .min(Comparator.comparingInt(shapeEntry -> shapeEntry.shape().size()))
        .orElseThrow();
  }

  /** Narrows this pool to shapes that have a room corpus, keeping the original weights. */
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
