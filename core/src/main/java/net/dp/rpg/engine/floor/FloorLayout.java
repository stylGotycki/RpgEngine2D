package net.dp.rpg.engine.floor;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public record FloorLayout(
    FloorGraph graph,
    RoomNode boss,
    List<RoomNode> criticalPath,
    int trunkRooms,
    int appendixRooms,
    int holeRooms,
    int extraDoors,
    int relocated,
    List<RoomType> unspentAbilities,
    WalkerLayout.StopReason walkStop) {

  public FloorLayout {
    criticalPath = List.copyOf(criticalPath);
    unspentAbilities = List.copyOf(unspentAbilities);
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

  public String summary() {
    return "rooms=%d cells=%d doors=%d deadEnds=%d critical=%d holes=%d extraDoors=%d types=%s%s".formatted(
        graph.size(), graph.usedCells(), graph.links().size(), graph.deadEnds().size(),
        criticalPath.size(), holeRooms, extraDoors, typeCounts(),
        unspentAbilities.isEmpty() ? "" : " UNSPENT=" + unspentAbilities);
  }
}
