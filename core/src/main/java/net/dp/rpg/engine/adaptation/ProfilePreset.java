package net.dp.rpg.engine.adaptation;

import java.util.EnumMap;
import java.util.Map;

// Typical players for the devtool and the experiments (axes -1..1). Only test points: the
// policy knows nothing about them, it reacts to the axis values.
public enum ProfilePreset {
  NEUTRAL("Neutralny", 0, 0, 0, 0, 0),
  FIGHTER("Wojownik", 0.9, -0.4, -0.3, -0.6, 0.3),
  EXPLORER("Odkrywca", -0.3, 0.9, 0.2, 0, 0.1),
  SOLVER("Zagadkowicz", -0.5, 0.2, 0.2, 0.9, 0),
  NOVICE("Nowicjusz", -0.2, -0.3, -0.5, -0.2, -0.8),
  VETERAN("Weteran", 0.3, 0.3, 0.3, 0.2, 0.9);

  private final String label;

  private final PlayerProfile profile;

  ProfilePreset(String label, double combat, double exploration, double caution, double puzzles,
      double skill) {
    Map<PlayStyle, Double> values = new EnumMap<>(PlayStyle.class);
    values.put(PlayStyle.COMBAT, combat);
    values.put(PlayStyle.EXPLORATION, exploration);
    values.put(PlayStyle.CAUTION, caution);
    values.put(PlayStyle.PUZZLES, puzzles);
    values.put(PlayStyle.SKILL, skill);
    this.label = label;
    this.profile = new PlayerProfile(values);
  }

  public String label() {
    return label;
  }

  public PlayerProfile profile() {
    return profile;
  }
}
