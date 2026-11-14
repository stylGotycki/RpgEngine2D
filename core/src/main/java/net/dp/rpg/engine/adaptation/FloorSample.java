package net.dp.rpg.engine.adaptation;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.content.ContentGenerator;
import net.dp.rpg.engine.content.ContentKind;
import net.dp.rpg.engine.content.ContentSpot;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.content.FloorContent;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorGenerator;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.engine.quest.Quest;
import net.dp.rpg.engine.quest.QuestContent;
import net.dp.rpg.engine.quest.QuestPlanner;
import net.dp.rpg.engine.tile.TileTypeRegistry;

// What one generated floor holds, counted the way a player meets it. The same pipeline as the
// game (floor, interiors, quests, content), so the devtool and experiments measure the real thing.
public record FloorSample(int rooms, int criticalLength, int deadEnds, int loops, int enemies,
    int fightingRooms, int minibosses, int shops, int puzzles, int potions, int healing,
    Map<GoalType, Integer> quests) {

  public FloorSample {
    Map<GoalType, Integer> copy = new EnumMap<>(GoalType.class);
    copy.putAll(quests);
    quests = Map.copyOf(copy);
  }

  public int questCount() {
    return quests.values().stream().mapToInt(Integer::intValue).sum();
  }

  public static FloorSample generate(FloorArchetype archetype, Adaptation adaptation, long seed,
      InteriorGenerator interiors, TileTypeRegistry types) {
    FloorLayout layout = new FloorGenerator().generate(archetype, adaptation.floor(), seed);
    Map<RoomNode, GeneratedRoom> generated = new IdentityHashMap<>();

    for (RoomNode node : layout.graph().rooms()) {
      generated.put(node, interiors.generate(layout.blueprintOf(node)));
    }

    List<Quest> planned = new QuestPlanner(adaptation.quests()).plan(layout);
    FloorContent content = new ContentGenerator(types, adaptation.content())
        .generate(layout, generated::get, planned);

    return of(layout, content, QuestContent.reconcile(planned, layout, content));
  }

  public static FloorSample of(FloorLayout layout, FloorContent content, List<Quest> quests) {
    int potions = 0;
    int healing = 0;
    int puzzles = 0;
    int fighting = 0;

    for (RoomNode room : layout.graph().rooms()) {
      for (ContentSpot spot : content.of(room).spots()) {
        boolean item = spot.kind() == ContentKind.ITEM;
        potions += item && spot.tag().equals(ContentTags.POTION) ? 1 : 0;
        healing += item && spot.tag().equals(ContentTags.HEALING_POTION) ? 1 : 0;
      }
      puzzles += content.of(room).puzzles().size();
      fighting += content.of(room).count(ContentKind.ENEMY) > 0 ? 1 : 0;
    }

    Map<GoalType, Integer> kinds = new EnumMap<>(GoalType.class);
    quests.forEach(quest -> kinds.merge(GoalType.valueOf(quest.template()), 1, Integer::sum));

    int size = layout.graph().size();
    return new FloorSample(size, layout.criticalLength(), layout.graph().deadEnds().size(),
        layout.graph().links().size() - (size - 1), content.total(ContentKind.ENEMY), fighting,
        count(layout, RoomType.MINIBOSS), count(layout, RoomType.SHOP), puzzles, potions + healing,
        healing, kinds);
  }

  private static int count(FloorLayout layout, RoomType type) {
    return (int) layout.graph().rooms().stream().filter(room -> room.type() == type).count();
  }
}
