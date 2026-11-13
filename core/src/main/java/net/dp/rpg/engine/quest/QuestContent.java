package net.dp.rpg.engine.quest;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.content.ContentKind;
import net.dp.rpg.engine.content.ContentSpot;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.content.FloorContent;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.puzzle.PuzzleKind;

// What the quests of a floor need in each room, and the goal counts once it has been placed.
public final class QuestContent {

  public record ItemRequest(String questId, String itemId) {
  }

  public record TalkerRequest(String questId, String dialogueId) {
  }

  public record PuzzleRequest(String questId, PuzzleKind kind) {
  }

  private QuestContent() {
  }

  // Least enemies of a tag the room must hold; KILL spreads its count over its rooms.
  public static Map<String, Integer> enemies(List<Quest> quests, int room) {
    Map<String, Integer> needed = new LinkedHashMap<>();

    for (Quest quest : quests) {
      for (QuestGoal goal : quest.goals()) {
        if (goal.type() == GoalType.KILL || goal.type() == GoalType.CHALLENGE) {
          int share = share(goal, room);

          if (share > 0) {
            needed.merge(goal.target(), share, Math::max);
          }
        }
      }
    }

    return needed;
  }

  public static List<ItemRequest> items(List<Quest> quests, int room) {
    List<ItemRequest> items = new ArrayList<>();

    for (Quest quest : quests) {
      for (QuestGoal goal : quest.goals()) {
        if (goal.type() == GoalType.COLLECT) {
          for (int i = 0; i < share(goal, room); i++) {
            items.add(new ItemRequest(quest.id(), goal.target()));
          }
        }
      }
    }

    return items;
  }

  public static List<TalkerRequest> talkers(List<Quest> quests, int room) {
    List<TalkerRequest> talkers = new ArrayList<>();

    for (Quest quest : quests) {
      for (QuestGoal goal : quest.goals()) {
        if (goal.type() == GoalType.TALK && goal.rooms().contains(room)) {
          talkers.add(new TalkerRequest(quest.id(), goal.target()));
        }
      }
    }

    return talkers;
  }

  public static List<PuzzleRequest> puzzles(List<Quest> quests, int room) {
    List<PuzzleRequest> puzzles = new ArrayList<>();

    for (Quest quest : quests) {
      for (QuestGoal goal : quest.goals()) {
        if (goal.type() == GoalType.PUZZLE && goal.rooms().contains(room)) {
          puzzles.add(new PuzzleRequest(quest.id(), PuzzleKind.valueOf(goal.target())));
        }
      }
    }

    return puzzles;
  }

  // The count over the goal's rooms in order, the first rooms taking the remainder.
  private static int share(QuestGoal goal, int room) {
    int position = goal.rooms().indexOf(room);

    if (position < 0) {
      return 0;
    }

    int rooms = goal.rooms().size();

    return goal.count() / rooms + (position < goal.count() % rooms ? 1 : 0);
  }

  // Goal counts as placed: KILL counts the target enemies in its rooms, CHALLENGE every enemy
  // of its room (it means clearing it), COLLECT the quest's items.
  public static List<Quest> reconcile(List<Quest> quests, FloorLayout layout,
      FloorContent content) {
    List<Quest> result = new ArrayList<>();

    for (Quest quest : quests) {
      List<QuestGoal> goals = new ArrayList<>();

      for (QuestGoal goal : quest.goals()) {
        int placed = 0;

        for (int index : goal.rooms()) {
          RoomNode room = layout.graph().rooms().get(index);

          for (ContentSpot spot : content.of(room).spots()) {
            placed += switch (goal.type()) {
              case KILL -> spot.kind() == ContentKind.ENEMY && spot.tag().equals(goal.target())
                  ? 1 : 0;
              case CHALLENGE -> spot.kind() == ContentKind.ENEMY ? 1 : 0;
              case COLLECT -> spot.kind() == ContentKind.ITEM
                  && spot.tag().equals(ContentTags.questItem(goal.target()))
                  && quest.id().equals(spot.questId()) ? 1 : 0;
              default -> 0;
            };
          }
        }

        boolean counted = goal.type() == GoalType.KILL || goal.type() == GoalType.CHALLENGE
            || goal.type() == GoalType.COLLECT;

        if (goal.type() == GoalType.PUZZLE) {
          goals.add(placedPuzzle(quest, goal, layout, content));
        } else {
          goals.add(counted ? goal.withCount(Math.max(1, placed)) : goal);
        }
      }

      result.add(quest.withGoals(goals));
    }

    return result;
  }

  // A tight room may hold another kind of puzzle than the planner chose.
  private static QuestGoal placedPuzzle(Quest quest, QuestGoal goal, FloorLayout layout,
      FloorContent content) {
    RoomNode room = layout.graph().rooms().get(goal.rooms().get(0));

    return content.of(room).puzzles().stream()
        .filter(puzzle -> quest.id().equals(puzzle.questId()))
        .findFirst()
        .map(puzzle -> new QuestGoal(goal.id(), goal.type(), puzzle.kind().name(), 1,
            goal.rooms(), goal.requires()))
        .orElse(goal);
  }
}
