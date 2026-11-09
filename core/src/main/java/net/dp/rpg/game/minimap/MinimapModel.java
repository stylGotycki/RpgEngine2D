package net.dp.rpg.game.minimap;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;

public final class MinimapModel {

  private final FloorLayout layout;

  private final Set<RoomNode> visited = new HashSet<>();

  private RoomNode current;

  public MinimapModel(FloorLayout layout) {
    this.layout = layout;
    enter(layout.graph().start());
  }

  public void enter(RoomNode room) {
    current = room;
    visited.add(room);
  }

  public void enter(int roomIndex) {
    enter(layout.graph().rooms().get(roomIndex));
  }

  public RoomNode current() {
    return current;
  }

  public boolean isVisited(RoomNode room) {
    return visited.contains(room);
  }

  public Set<RoomNode> visited() {
    return Collections.unmodifiableSet(visited);
  }

  public boolean isRevealed(RoomNode room) {
    if (visited.contains(room)) {
      return true;
    }

    for (RoomLink link : room.links()) {
      if (visited.contains(link.other(room)) && link.doorType() != DoorType.SECRET) {
        return true;
      }
    }

    return false;
  }

  public List<RoomNode> revealedRooms() {
    List<RoomNode> rooms = new ArrayList<>();

    for (RoomNode room : layout.graph().rooms()) {
      if (isRevealed(room)) {
        rooms.add(room);
      }
    }

    return rooms;
  }

  public List<RoomLink> knownDoors() {
    List<RoomLink> doors = new ArrayList<>();

    for (RoomLink link : layout.graph().links()) {
      boolean fromSeen = visited.contains(link.from());
      boolean toSeen = visited.contains(link.to());
      boolean known = link.doorType() == DoorType.SECRET ? fromSeen && toSeen : fromSeen || toSeen;

      if (known) {
        doors.add(link);
      }
    }

    return doors;
  }

  public boolean owns(RoomNode room) {
    List<RoomNode> rooms = layout.graph().rooms();

    return room != null && room.index() >= 0 && room.index() < rooms.size()
        && rooms.get(room.index()) == room;
  }

  public FloorLayout layout() {
    return layout;
  }
}
