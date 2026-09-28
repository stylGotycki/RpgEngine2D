package net.dp.rpg.engine.floor;

import java.util.Comparator;
import java.util.List;
import net.dp.rpg.engine.floor.graph.RoomNode;

public record QuestBundle(String id, List<RoomNode> rooms) {

  public QuestBundle {
    if (rooms.isEmpty()) {
      throw new IllegalArgumentException("A quest bundle needs at least one room: " + id);
    }

    rooms = List.copyOf(rooms);
  }

  public int size() {
    return rooms.size();
  }

  public RoomNode entrance() {
    return rooms.stream().min(Comparator.comparingInt(RoomNode::depth)).orElseThrow();
  }

  public boolean contains(RoomNode room) {
    return rooms.contains(room);
  }
}