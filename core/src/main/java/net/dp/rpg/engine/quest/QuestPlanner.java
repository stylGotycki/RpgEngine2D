package net.dp.rpg.engine.quest;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.QuestBundle;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.puzzle.PuzzleKind;

public final class QuestPlanner {

  public static final List<String> COLLECTIBLES = List.of("quest_relic", "quest_ore", "quest_herb");

  public static final List<String> TALKS = List.of("talk.q.sentinel", "talk.q.riddle");

  private final QuestWeights weights;

  public QuestPlanner(QuestWeights weights) {
    this.weights = weights;
  }

  public List<Quest> plan(FloorLayout layout) {
    int maxDepth = layout.graph().rooms().stream().mapToInt(RoomNode::depth).max().orElse(1);
    List<Quest> quests = new ArrayList<>();

    for (QuestBundle bundle : layout.quests()) {
      Random random = RandomSource.derive(layout.seed(), "quest:" + bundle.id());
      Quest quest = plan(bundle, maxDepth, random);

      if (quest != null) {
        quests.add(quest);
      }
    }

    return quests;
  }

  private Quest plan(QuestBundle bundle, int maxDepth, Random random) {
    RoomNode entrance = bundle.entrance();
    // Deepest first: the far end of the bundle is where a quest should lead.
    List<RoomNode> others = bundle.rooms().stream().filter(room -> room != entrance)
        .sorted(Comparator.comparingInt(RoomNode::depth).reversed()
            .thenComparingInt(RoomNode::index))
        .toList();
    double depth = maxDepth == 0 ? 0 : (double) entrance.depth() / maxDepth;
    GoalType type = pickType(!others.isEmpty(), random);

    if (type == null) {
      return null;
    }

    String enemy = random.nextDouble() < 0.15 + 0.5 * depth ? ContentTags.SKELETON
        : ContentTags.SPIDER;
    List<QuestGoal> goals = switch (type) {
      case KILL -> List.of(new QuestGoal("g1", GoalType.KILL, enemy, 3 + random.nextInt(3),
          indices(others), List.of()));
      case COLLECT -> List.of(new QuestGoal("g1", GoalType.COLLECT,
          COLLECTIBLES.get(random.nextInt(COLLECTIBLES.size())), 2 + random.nextInt(2),
          indices(others.isEmpty() ? List.of(entrance) : others), List.of()));
      case REACH -> List.of(new QuestGoal("g1", GoalType.REACH, null, 1,
          List.of(others.get(0).index()), List.of()));
      case CHALLENGE -> List.of(new QuestGoal("g1", GoalType.CHALLENGE, enemy, 3,
          List.of(others.get(random.nextInt(others.size())).index()), List.of()));
      case TALK -> List.of(new QuestGoal("g1", GoalType.TALK,
          TALKS.get(random.nextInt(TALKS.size())), 1,
          List.of((others.isEmpty() ? entrance : others.get(0)).index()), List.of()));
      case PUZZLE -> List.of(new QuestGoal("g1", GoalType.PUZZLE,
          PuzzleKind.values()[random.nextInt(PuzzleKind.values().length)].name(), 1,
          List.of((others.isEmpty() ? entrance : others.get(0)).index()), List.of()));
    };

    return new Quest(bundle.id(), type.name(), entrance.index(), goals, ContentTags.EQUIPMENT,
        depth);
  }

  private GoalType pickType(boolean hasOtherRooms, Random random) {
    List<GoalType> feasible = new ArrayList<>();

    for (GoalType type : GoalType.values()) {
      boolean needsRooms = type == GoalType.KILL || type == GoalType.REACH
          || type == GoalType.CHALLENGE;

      if (weights.of(type) > 0 && (hasOtherRooms || !needsRooms)) {
        feasible.add(type);
      }
    }

    double total = feasible.stream().mapToDouble(weights::of).sum();
    // The roll is drawn even when nothing is feasible, so the stream stays in step.
    double roll = random.nextDouble() * total;

    for (GoalType type : feasible) {
      roll -= weights.of(type);
      if (roll < 0) {
        return type;
      }
    }

    return feasible.isEmpty() ? null : feasible.get(feasible.size() - 1);
  }

  private static List<Integer> indices(List<RoomNode> rooms) {
    return rooms.stream().map(RoomNode::index).toList();
  }
}
