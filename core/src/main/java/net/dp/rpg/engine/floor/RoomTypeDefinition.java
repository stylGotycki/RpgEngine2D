package net.dp.rpg.engine.floor;

public record RoomTypeDefinition(
    RoomType type,
    TypePolicy policy,
    ShapePool shapes,
    AbilityPhase phase,
    SlotPreference slot,
    boolean relocatable) {

  public static RoomTypeDefinition filler(RoomType type, double weight) {
    return new RoomTypeDefinition(type, new TypePolicy.FillerWeight(weight), null,
        AbilityPhase.TRUNK, SlotPreference.ANY, false);
  }

  public ShapePool shapesOr(ShapePool fallback) {
    return shapes == null ? fallback : shapes;
  }

  public boolean isFiller() {
    return policy instanceof TypePolicy.FillerWeight;
  }

  public boolean isDisabled() {
    return policy instanceof TypePolicy.Disabled;
  }

  public double fillerWeight() {
    return policy instanceof TypePolicy.FillerWeight(double weight) ? weight : 0.0;
  }
}
