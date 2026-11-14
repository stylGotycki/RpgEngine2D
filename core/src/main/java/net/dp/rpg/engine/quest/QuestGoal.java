package net.dp.rpg.engine.quest;

import java.util.List;

public record QuestGoal(String id, GoalType type, String target, int count, List<Integer> rooms,
    List<String> requires) {

  public QuestGoal {
    if (count < 1) {
      throw new IllegalArgumentException("A goal needs a count of at least 1: " + id);
    }
    rooms = List.copyOf(rooms);
    requires = List.copyOf(requires);
  }

  public QuestGoal withCount(int value) {
    return new QuestGoal(id, type, target, value, rooms, requires);
  }
}
