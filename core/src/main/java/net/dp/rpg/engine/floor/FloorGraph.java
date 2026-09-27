package net.dp.rpg.engine.floor;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class FloorGraph {

  private final Map<RoomCell, RoomNode> occupied = new LinkedHashMap<>();

  private final Set<RoomCell> reserved = new LinkedHashSet<>();

  private final List<RoomNode> rooms = new ArrayList<>();

  private final List<RoomLink> links = new ArrayList<>();

  private RoomNode start;

  private int exemptCells;

  public boolean isFree(RoomCell cell) {
    return !occupied.containsKey(cell) && !reserved.contains(cell);
  }

  public boolean isOccupied(RoomCell cell) {
    return occupied.containsKey(cell);
  }

  public RoomNode roomAt(RoomCell cell) {
    return occupied.get(cell);
  }

  public boolean canPlace(RoomShape shape, RoomCell origin, GridBounds bounds) {
    for (RoomCell cell : shape.cells()) {
      RoomCell floorCell = cell.translated(origin.x(), origin.y());

      if (!bounds.contains(floorCell) || !isFree(floorCell)) {
        return false;
      }
    }

    return true;
  }

  public RoomNode place(RoomShape shape, RoomCell origin) {
    RoomNode node = new RoomNode(rooms.size(), shape, origin);

    for (RoomCell cell : node.cells()) {
      RoomNode previous = occupied.put(cell, node);

      if (previous != null) {
        throw new IllegalStateException("Cell %d,%d already belongs to %s".formatted(cell.x(), cell.y(), previous));
      }
    }

    rooms.add(node);

    if (start == null) {
      start = node;
    }

    return node;
  }

  public void reserve(RoomCell cell) {
    reserved.add(cell);
  }

  public void release(RoomCell cell) {
    reserved.remove(cell);
  }

  public boolean isReserved(RoomCell cell) {
    return reserved.contains(cell);
  }

  public RoomLink connect(RoomNode from, RoomNode to, RoomEdge edge, DoorType doorType) {
    if (from == to) {
      throw new IllegalArgumentException("A room cannot link to itself: " + from);
    }

    RoomLink link = new RoomLink(from, to, edge, doorType);

    links.add(link);
    from.addLink(link);
    to.addLink(link);

    return link;
  }

  public void computeDepths() {
    rooms.forEach(room -> room.setDepth(-1));

    if (start == null) {
      return;
    }

    Deque<RoomNode> queue = new ArrayDeque<>();

    start.setDepth(0);
    queue.add(start);

    while (!queue.isEmpty()) {
      RoomNode current = queue.poll();

      for (RoomLink link : current.links()) {
        RoomNode next = link.other(current);

        if (next.depth() < 0) {
          next.setDepth(current.depth() + 1);
          queue.add(next);
        }
      }
    }
  }

  public boolean isConnected() {
    computeDepths();

    return rooms.stream().allMatch(room -> room.depth() >= 0);
  }

  public List<RoomNode> deadEnds() {
    return rooms.stream().filter(RoomNode::isDeadEnd).toList();
  }

  public RoomNode deepest() {
    return rooms.stream().max((first, second) -> Integer.compare(first.depth(), second.depth())).orElse(null);
  }

  public List<RoomNode> pathToStart(RoomNode target) {
    List<RoomNode> path = new ArrayList<>();
    RoomNode current = target;

    while (current != null && current != start) {
      path.add(current);

      RoomNode next = null;

      for (RoomLink link : current.links()) {
        RoomNode candidate = link.other(current);

        if (candidate.depth() == current.depth() - 1) {
          next = candidate;
          break;
        }
      }

      current = next;
    }

    if (current == start) {
      path.add(start);
    }

    Collections.reverse(path);

    return path;
  }

  public void markExempt(RoomNode room) {
    exemptCells += room.size();
  }

  public int usedCells() {
    return occupied.size();
  }

  public int budgetedCells() {
    return occupied.size() - exemptCells;
  }

  public int size() {
    return rooms.size();
  }

  public List<RoomNode> rooms() {
    return Collections.unmodifiableList(rooms);
  }

  public List<RoomLink> links() {
    return Collections.unmodifiableList(links);
  }

  public RoomNode start() {
    return start;
  }
}
