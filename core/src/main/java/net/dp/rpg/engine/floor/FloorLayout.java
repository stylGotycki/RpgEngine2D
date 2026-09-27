package net.dp.rpg.engine.floor;

import java.util.List;

public record FloorLayout(
    FloorGraph graph,
    RoomNode boss,
    List<RoomNode> criticalPath,
    int trunkRooms,
    int appendixRooms,
    int holeRooms,
    int extraDoors,
    WalkerLayout.StopReason walkStop) {

  public FloorLayout {
    criticalPath = List.copyOf(criticalPath);
  }

  public int rooms() {
    return graph.size();
  }

  public int criticalLength() {
    return criticalPath.size();
  }

  public String summary() {
    return "rooms=%d cells=%d doors=%d deadEnds=%d critical=%d holes=%d extraDoors=%d walk=%s".formatted(
        graph.size(), graph.usedCells(), graph.links().size(), graph.deadEnds().size(),
        criticalPath.size(), holeRooms, extraDoors, walkStop);
  }
}
