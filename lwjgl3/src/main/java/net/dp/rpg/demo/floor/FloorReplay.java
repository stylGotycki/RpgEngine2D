package net.dp.rpg.demo.floor;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.tile.room.RoomCell;

public final class FloorReplay {

  private final FloorLayout layout;

  private final List<RoomCell> path;

  private final Set<Integer> revealed = new LinkedHashSet<>();

  private RoomCell walker;

  private int pathIndex;

  public FloorReplay(FloorLayout layout) {
    this.layout = layout;
    this.path = layout.walkerPath();
  }

  public boolean finished() {
    return pathIndex >= path.size() && revealed.size() >= layout.graph().size();
  }

  public boolean advance() {
    if (pathIndex < path.size()) {
      walker = path.get(pathIndex++);

      RoomNode room = layout.graph().roomAt(walker);

      if (room != null) {
        revealed.add(room.index());
      }

      return true;
    }

    walker = null;

    for (RoomNode room : layout.graph().rooms()) {
      if (revealed.add(room.index())) {
        return true;
      }
    }

    return false;
  }

  public void finish() {
    walker = null;
    pathIndex = path.size();

    layout.graph().rooms().forEach(room -> revealed.add(room.index()));
  }

  public void restart() {
    revealed.clear();
    walker = null;
    pathIndex = 0;
  }

  public FloorReveal reveal() {
    return new FloorReveal(revealed, walker, finished());
  }

  public int step() {
    return Math.min(pathIndex, path.size()) + Math.max(0, revealed.size() - roomsOnPath());
  }

  public int totalSteps() {
    return path.size() + layout.graph().size() - roomsOnPath();
  }

  public String stage() {
    if (finished()) {
      return "done";
    }

    return pathIndex < path.size() ? "walk" : "spurs";
  }

  private int roomsOnPath() {
    Set<Integer> onPath = new LinkedHashSet<>();

    for (RoomCell cell : path) {
      RoomNode room = layout.graph().roomAt(cell);

      if (room != null) {
        onPath.add(room.index());
      }
    }

    return onPath.size();
  }
}
