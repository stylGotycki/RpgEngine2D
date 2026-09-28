package net.dp.rpg.engine.floor.type;

public sealed interface TypePolicy {

  record Exactly(int count) implements TypePolicy {
  }

  record AtMost(int count) implements TypePolicy {
  }

  record PerFloorChance(double chance) implements TypePolicy {
  }

  record FillerWeight(double weight) implements TypePolicy {
  }

  record Disabled() implements TypePolicy {
  }
}
