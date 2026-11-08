package net.dp.rpg.engine.floor;

import java.util.ArrayList;
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

  public GridBounds extent() {
    RoomCell min = graph.minCorner();
    int maxX = min.x();
    int maxY = min.y();

    for (RoomNode room : graph.rooms()) {
      for (RoomCell cell : room.cells()) {
        maxX = Math.max(maxX, cell.x());
        maxY = Math.max(maxY, cell.y());
      }
    }

    return new GridBounds(maxX - min.x() + 1, maxY - min.y() + 1);
  }

  public boolean isNormalized() {
    return graph.minCorner().equals(RoomCell.ORIGIN);
  }

  public FloorLayout normalized() {
    RoomCell min = graph.minCorner();

    if (min.equals(RoomCell.ORIGIN)) {
      return this;
    }

    int deltaX = -min.x();
    int deltaY = -min.y();
    FloorGraph moved = graph.translated(deltaX, deltaY);
    List<RoomCell> movedPath = new ArrayList<>(walkerPath.size());

    walkerPath.forEach(cell -> movedPath.add(cell.translated(deltaX, deltaY)));

    List<QuestBundle> movedQuests = new ArrayList<>(quests.size());

    for (QuestBundle bundle : quests) {
      movedQuests.add(new QuestBundle(bundle.id(), sameRooms(moved, bundle.rooms())));
    }

    return new FloorLayout(archetypeId, seed, moved,
        boss == null ? null : moved.rooms().get(boss.index()),
        sameRooms(moved, criticalPath), trunkRooms, appendixRooms, holeRooms, extraDoors,
        relocated, unspentAbilities, movedPath, movedQuests, walkStop);
  }

  private static List<RoomNode> sameRooms(FloorGraph graph, List<RoomNode> rooms) {
    return rooms.stream().map(room -> graph.rooms().get(room.index())).toList();
  }

  public String summary() {
    return ("rooms=%d cells=%d doors=%d deadEnds=%d critical=%d holes=%d extraDoors=%d "
        + "quests=%d/%d types=%s%s").formatted(
        graph.size(), graph.usedCells(), graph.links().size(), graph.deadEnds().size(),
        criticalPath.size(), holeRooms, extraDoors, quests.size(), questRooms(), typeCounts(),
        unspentAbilities.isEmpty() ? "" : " UNSPENT=" + unspentAbilities);
  }
}
