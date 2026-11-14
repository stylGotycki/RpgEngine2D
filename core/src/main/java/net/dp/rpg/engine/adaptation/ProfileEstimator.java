package net.dp.rpg.engine.adaptation;

import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.engine.quest.GoalType;

// Turns one floor's metrics into a profile. Every axis is first a share in 0..1 and then maps to
// -1..1. A ratio with nothing to measure (no puzzles on the floor, no quests taken) counts as
// 0.5, i.e. 0 on the axis, so missing content does not read as a preference.
public final class ProfileEstimator {

  // Damage per visited room that reads as "not careful at all".
  private static final double CARELESS_DAMAGE_PER_ROOM = 25;

  // Damage per kill that reads as "no skill"; a kill without a scratch reads as full skill.
  private static final double UNSKILLED_DAMAGE_PER_KILL = 12;

  private static final double NO_EVIDENCE = 0.5;

  public PlayerProfile estimate(FloorMetrics floor) {
    Map<PlayStyle, Double> values = new EnumMap<>(PlayStyle.class);
    double killed = ratio(floor.enemiesKilled(), floor.enemiesTotal());
    double visited = ratio(floor.roomsVisited(), floor.roomsTotal());
    double picked = ratio(floor.itemsPicked(), floor.itemsTotal());
    double solved = ratio(floor.puzzlesSolved(), floor.puzzlesTotal());
    double answered = ratio(floor.talkCorrect(), floor.talkCorrect() + floor.talkWrong());
    int accepted = floor.accepted();

    values.put(PlayStyle.COMBAT, mix(killed, 0.7,
        ratio(floor.accepted(GoalType.KILL, GoalType.CHALLENGE), accepted), 0.3));
    values.put(PlayStyle.EXPLORATION, mix(mix(visited, 0.6, picked, 0.4), 0.8,
        ratio(floor.accepted(GoalType.REACH), accepted), 0.2));

    double damagePerRoom = floor.damageTaken() / Math.max(1, floor.roomsVisited());
    double potions = Math.min(1, floor.potionsUsed() / 3.0);
    values.put(PlayStyle.CAUTION,
        0.7 * (1 - Math.min(1, damagePerRoom / CARELESS_DAMAGE_PER_ROOM)) + 0.3 * potions);

    values.put(PlayStyle.PUZZLES, mix(mix(solved, 0.7, answered, 0.3), 0.7,
        ratio(floor.accepted(GoalType.PUZZLE, GoalType.TALK), accepted), 0.3));

    double skill = floor.enemiesKilled() == 0 ? NO_EVIDENCE
        : 1 - Math.min(1, floor.damageTaken() / floor.enemiesKilled() / UNSKILLED_DAMAGE_PER_KILL);
    values.put(PlayStyle.SKILL, skill);

    values.replaceAll((style, share) -> 2 * share - 1);
    return new PlayerProfile(values);
  }

  private static double ratio(double part, double whole) {
    return whole <= 0 ? NO_EVIDENCE : Math.min(1, part / whole);
  }

  private static double mix(double a, double weightA, double b, double weightB) {
    return (a * weightA + b * weightB) / (weightA + weightB);
  }
}
