package net.dp.rpg.engine.quest;

import java.util.EnumMap;
import java.util.Map;

public record QuestWeights(Map<GoalType, Double> weights) {

  public static final QuestWeights DEFAULTS = new QuestWeights(Map.of(
      GoalType.KILL, 1.0, GoalType.COLLECT, 1.0, GoalType.REACH, 0.7,
      GoalType.CHALLENGE, 0.8, GoalType.TALK, 0.9, GoalType.PUZZLE, 0.0));

  public QuestWeights {
    weights = Map.copyOf(new EnumMap<>(weights));
  }

  public double of(GoalType type) {
    return weights.getOrDefault(type, 0.0);
  }

  public QuestWeights with(GoalType type, double weight) {
    Map<GoalType, Double> changed = new EnumMap<>(GoalType.class);

    changed.putAll(weights);
    changed.put(type, Math.max(0, weight));

    return new QuestWeights(changed);
  }

  public static QuestWeights only(GoalType type) {
    Map<GoalType, Double> single = new EnumMap<>(GoalType.class);

    for (GoalType each : GoalType.values()) {
      single.put(each, each == type ? 1.0 : 0.0);
    }

    return new QuestWeights(single);
  }
}
