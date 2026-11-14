package net.dp.rpg.engine.adaptation;

import java.util.EnumMap;
import java.util.Map;

// Each axis in -1..1: -1 avoids it, 1 seeks it, 0 means "no evidence either way" and is where
// every player starts.
public record PlayerProfile(Map<PlayStyle, Double> values) {

  public static final double NEUTRAL_VALUE = 0;

  public static final double MIN = -1;

  public static final double MAX = 1;

  public static final PlayerProfile NEUTRAL = uniform(NEUTRAL_VALUE);

  public PlayerProfile {
    Map<PlayStyle, Double> copy = new EnumMap<>(PlayStyle.class);

    for (PlayStyle style : PlayStyle.values()) {
      copy.put(style, clamp(values.getOrDefault(style, NEUTRAL_VALUE)));
    }
    values = Map.copyOf(copy);
  }

  public static PlayerProfile uniform(double value) {
    Map<PlayStyle, Double> values = new EnumMap<>(PlayStyle.class);

    for (PlayStyle style : PlayStyle.values()) {
      values.put(style, value);
    }

    return new PlayerProfile(values);
  }

  public double get(PlayStyle style) {
    return values.get(style);
  }

  public PlayerProfile with(PlayStyle style, double value) {
    Map<PlayStyle, Double> changed = new EnumMap<>(values);
    changed.put(style, value);
    return new PlayerProfile(changed);
  }

  // Exponential smoothing: one floor moves the profile by `weight` towards what it showed.
  public PlayerProfile blend(PlayerProfile measured, double weight) {
    Map<PlayStyle, Double> blended = new EnumMap<>(PlayStyle.class);

    for (PlayStyle style : PlayStyle.values()) {
      blended.put(style, get(style) * (1 - weight) + measured.get(style) * weight);
    }

    return new PlayerProfile(blended);
  }

  static double clamp(double value) {
    return Double.isFinite(value) ? Math.max(MIN, Math.min(MAX, value)) : NEUTRAL_VALUE;
  }
}
