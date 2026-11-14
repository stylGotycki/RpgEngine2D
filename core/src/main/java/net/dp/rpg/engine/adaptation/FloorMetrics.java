package net.dp.rpg.engine.adaptation;

import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.engine.quest.GoalType;

public record FloorMetrics(
    int roomsTotal,
    int roomsVisited,
    int enemiesTotal,
    int enemiesKilled,
    int itemsTotal,
    int itemsPicked,
    int puzzlesTotal,
    int puzzlesSolved,
    int talkCorrect,
    int talkWrong,
    float damageTaken,
    int potionsUsed,
    int questsOffered,
    Map<GoalType, Integer> questsAccepted,
    int questsDeclined,
    int questsCompleted,
    double seconds) {

  public FloorMetrics {
    Map<GoalType, Integer> copy = new EnumMap<>(GoalType.class);
    copy.putAll(questsAccepted);
    questsAccepted = Map.copyOf(copy);
  }

  public int accepted() {
    return questsAccepted.values().stream().mapToInt(Integer::intValue).sum();
  }

  public int accepted(GoalType... types) {
    int total = 0;

    for (GoalType type : types) {
      total += questsAccepted.getOrDefault(type, 0);
    }

    return total;
  }
}
