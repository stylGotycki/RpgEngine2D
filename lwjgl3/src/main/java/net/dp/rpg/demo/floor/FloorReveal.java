package net.dp.rpg.demo.floor;

import java.util.LinkedHashSet;
import java.util.Set;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.tile.room.RoomCell;


public record FloorReveal(Set<Integer> rooms, RoomCell walker, boolean complete) {

  public FloorReveal {
    rooms = Set.copyOf(rooms);
  }

  public static FloorReveal everything(FloorLayout layout) {
    Set<Integer> all = new LinkedHashSet<>();

    layout.graph().rooms().forEach(room -> all.add(room.index()));

    return new FloorReveal(all, null, true);
  }

  public boolean shows(RoomNode room) {
    return rooms.contains(room.index());
  }

  public boolean shows(RoomLink link) {
    return rooms.contains(link.from().index()) && rooms.contains(link.to().index());
  }

  public boolean isWalker(RoomCell cell) {
    return walker != null && walker.equals(cell);
  }
}
