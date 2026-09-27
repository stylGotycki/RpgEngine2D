package net.dp.rpg.engine.floor;

import java.util.List;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class FloorBuilder {

  private final WalkerLayout walker = new WalkerLayout();

  private final AppendixPhase appendices = new AppendixPhase();

  private final HolePhase holes = new HolePhase();

  public FloorLayout build(FloorPlan plan, GridBounds bounds, WalkerSettings settings, long seed) {
    WalkerLayout.WalkResult walk = walker.grow(plan.trunkPlan(), bounds, settings, seed);
    FloorGraph graph = walk.graph();

    ShapeDrawContext shapes = new ShapeDrawContext(plan.exclusiveGroups());

    graph.rooms().forEach(room -> shapes.confirm(room.variant()));

    AppendixPhase.Result attached = appendices.attach(graph, plan, shapes, bounds,
        RandomSource.derive(seed, "anchor"), RandomSource.derive(seed, "appendix"));

    int holeRooms = holes.fill(graph, bounds,
        RandomSource.derive(seed, "hole"), RandomSource.derive(seed, "holeShape"));

    int extraDoors = walk.extraDoors() + completeDoorways(graph);

    graph.computeDepths();

    RoomNode boss = attached.boss();
    List<RoomNode> criticalPath = boss == null ? List.of() : graph.pathToStart(boss);

    return new FloorLayout(graph, boss, criticalPath, walk.rooms(), attached.attached(), holeRooms,
        extraDoors, walk.reason());
  }

  private int completeDoorways(FloorGraph graph) {
    int opened = 0;

    for (RoomNode room : List.copyOf(graph.rooms())) {
      for (RoomEdge edge : room.outerEdges()) {
        RoomNode other = graph.roomAt(edge.cell().neighbour(edge.direction()));

        if (other == null || other.index() < room.index() || !room.isLinkedTo(other)) {
          continue;
        }

        if (graph.doorAt(edge.cell(), edge.direction()) != null || isLocked(room, other)) {
          continue;
        }

        graph.connect(room, other, edge, DoorType.NORMAL);
        opened++;
      }
    }

    return opened;
  }

  private boolean isLocked(RoomNode room, RoomNode other) {
    return room.links().stream()
        .anyMatch(link -> link.other(room) == other && link.doorType() == DoorType.LOCKED);
  }
}
