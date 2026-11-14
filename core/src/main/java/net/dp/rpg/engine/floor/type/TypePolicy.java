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

  // An expected count: the whole part always, one more with the fraction as chance. Draws a
  // number only when there is a fraction, so Expected(1) behaves exactly like Exactly(1).
  record Expected(double count) implements TypePolicy {

    public Expected {
      if (!Double.isFinite(count) || count < 0) {
        throw new IllegalArgumentException("count must be zero or more, got " + count);
      }
    }

    public int roll(java.util.Random random) {
      int whole = (int) Math.floor(count);
      double fraction = count - whole;

      return whole + (fraction > 0 && random.nextDouble() < fraction ? 1 : 0);
    }
  }
}
