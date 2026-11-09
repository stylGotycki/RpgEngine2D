package net.dp.rpg.engine.content;

import java.util.EnumMap;
import java.util.IdentityHashMap;
import java.util.Map;
import net.dp.rpg.engine.floor.graph.RoomNode;

/** The content of every room of a floor, by room. */
public final class FloorContent {

  private final Map<RoomNode, RoomContent> rooms;

  FloorContent(Map<RoomNode, RoomContent> rooms) {
    this.rooms = new IdentityHashMap<>(rooms);
  }

  public RoomContent of(RoomNode room) {
    return rooms.getOrDefault(room, RoomContent.EMPTY);
  }

  public int total(ContentKind kind) {
    return rooms.values().stream().mapToInt(content -> content.count(kind)).sum();
  }

  public Map<ContentKind, Integer> totals() {
    Map<ContentKind, Integer> totals = new EnumMap<>(ContentKind.class);

    for (ContentKind kind : ContentKind.values()) {
      totals.put(kind, total(kind));
    }

    return totals;
  }
}
