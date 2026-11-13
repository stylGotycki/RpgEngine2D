package net.dp.rpg.engine.quest;

import java.util.List;

public record Quest(String id, String template, int giverRoom, List<QuestGoal> goals,
    String rewardTag, double depth) {

  public Quest {
    goals = List.copyOf(goals);
  }

  public QuestGoal goal(String goalId) {
    return goals.stream().filter(goal -> goal.id().equals(goalId)).findFirst()
        .orElseThrow(() -> new IllegalArgumentException(id + " has no goal " + goalId));
  }

  public Quest withGoals(List<QuestGoal> value) {
    return new Quest(id, template, giverRoom, value, rewardTag, depth);
  }
}
