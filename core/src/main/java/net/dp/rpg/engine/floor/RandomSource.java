package net.dp.rpg.engine.floor;

import java.util.Random;

public final class RandomSource {

  private static final long GOLDEN_GAMMA = 0x9E3779B97F4A7C15L;

  private static final long FNV_OFFSET = 0xCBF29CE484222325L;

  private static final long FNV_PRIME = 0x100000001B3L;

  private RandomSource() {
  }

  public static Random derive(long seed, String label) {
    return new Random(deriveSeed(seed, label));
  }

  public static long deriveSeed(long seed, String label) {
    return mix(seed ^ hash(label));
  }

  public static long mix(long value) {
    long z = value + GOLDEN_GAMMA;

    z = (z ^ (z >>> 30)) * 0xBF58476D1CE4E5B9L;
    z = (z ^ (z >>> 27)) * 0x94D049BB133111EBL;

    return z ^ (z >>> 31);
  }

  public static long hash(String label) {
    long hash = FNV_OFFSET;

    for (int index = 0; index < label.length(); index++) {
      hash = (hash ^ label.charAt(index)) * FNV_PRIME;
    }

    return hash;
  }
}
