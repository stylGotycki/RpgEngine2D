package net.dp.rpg.game.item;

import net.dp.rpg.game.SkillType;

public enum Stat {
  STRENGTH("SIL"),
  AGILITY("ZRE"),
  WISDOM("INT"),
  DEFENSE("OBR");

  private final String shortName;

  Stat(String shortName) {
    this.shortName = shortName;
  }

  public String shortName() {
    return shortName;
  }

  public static Stat of(SkillType skill) {
    return switch (skill) {
      case STRENGTH -> STRENGTH;
      case AGILITY -> AGILITY;
      case WISDOM -> WISDOM;
    };
  }
}
