package net.dp.rpg.game.quest;

import java.util.Random;

public final class QuestRewards {

  public enum Kind {
    POTION,
    SKILL_POINT,
    HEAL,
    EQUIPMENT
  }

  // Mostly potions, free healing and free skill points; equipment is the rare treat.
  private static final double[] WEIGHTS = {0.45, 0.25, 0.20, 0.10};

  private QuestRewards() {
  }

  public static Kind roll(Random random) {
    double roll = random.nextDouble();

    for (Kind kind : Kind.values()) {
      roll -= WEIGHTS[kind.ordinal()];
      if (roll < 0) {
        return kind;
      }
    }

    return Kind.POTION;
  }
}
