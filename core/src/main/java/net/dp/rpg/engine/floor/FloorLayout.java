package net.dp.rpg.engine.floor;

import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.phase.WalkerLayout;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.RoomCell;

public record FloorLayout(
    String archetypeId,
    long seed,
    FloorGraph graph,
    RoomNode boss,
    List<RoomNode> criticalPath,
    int trunkRooms,
    int appendixRooms,
    int holeRooms,
    int extraDoors,
    int relocated,
    List<RoomType> unspentAbilities,
    List<RoomCell> walkerPath,
    List<QuestBundle> quests,
    WalkerLayout.StopReason walkStop) {

  public FloorLayout {
    criticalPath = List.copyOf(criticalPath);
    unspentAbilities = List.copyOf(unspentAbilities);
    walkerPath = List.copyOf(walkerPath);
    quests = List.copyOf(quests);
  }

  public int rooms() {
    return graph.size();
  }

  public int criticalLength() {
    return criticalPath.size();
  }

  public RoomNode roomOfType(RoomType type) {
    return graph.rooms().stream().filter(room -> room.type() == type).findFirst().orElse(null);
  }

  public Map<RoomType, Integer> typeCounts() {
    Map<RoomType, Integer> counts = new EnumMap<>(RoomType.class);

    graph.rooms().forEach(room -> counts.merge(room.type(), 1, Integer::sum));

    return counts;
  }

  public RoomBlueprint blueprintOf(RoomNode room) {
    return RoomBlueprint.of(room, seed);
  }

  public List<RoomBlueprint> blueprints() {
    return graph.rooms().stream()
        .map(this::blueprintOf)
        .sorted(Comparator.comparing(RoomBlueprint::anchor, RoomBlueprint.CELL_ORDER))
        .toList();
  }

  public int questRooms() {
    return quests.stream().mapToInt(QuestBundle::size).sum();
  }

  public QuestBundle questOf(RoomNode room) {
    return quests.stream().filter(bundle -> bundle.contains(room)).findFirst().orElse(null);
  }

  public String summary() {
    return ("rooms=%d cells=%d doors=%d deadEnds=%d critical=%d holes=%d extraDoors=%d "
        + "quests=%d/%d types=%s%s").formatted(
        graph.size(), graph.usedCells(), graph.links().size(), graph.deadEnds().size(),
        criticalPath.size(), holeRooms, extraDoors, quests.size(), questRooms(), typeCounts(),
        unspentAbilities.isEmpty() ? "" : " UNSPENT=" + unspentAbilities);
  }
}
