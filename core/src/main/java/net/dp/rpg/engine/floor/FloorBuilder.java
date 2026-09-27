package net.dp.rpg.engine.floor;

import java.util.List;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class FloorBuilder {

  private final WalkerLayout walker = new WalkerLayout();

  private final AppendixPhase appendices = new AppendixPhase();

  private final HolePhase holes = new HolePhase();

  private final RetrospectivePass retrospective = new RetrospectivePass();

  public FloorLayout build(FloorPlan plan, GridBounds bounds, WalkerSettings settings, long seed) {
    return build(FloorContext.of(plan, bounds, settings, seed));
  }

  public FloorLayout build(FloorContext context) {
    WalkerLayout.WalkResult walk = walker.grow(context);
    FloorGraph graph = walk.graph();

    ShapeDrawContext shapes = new ShapeDrawContext(context.plan().exclusiveGroups());

    graph.rooms().forEach(room -> shapes.confirm(room.variant()));

    AppendixPhase.Result attached = appendices.attach(graph, context, shapes);
    int holeRooms = holes.fill(graph, context);
    int extraDoors = walk.extraDoors() + completeDoorways(graph);

    graph.computeDepths();

    RoomNode boss = attached.boss();
    List<RoomNode> criticalPath = boss == null ? List.of() : graph.pathToStart(boss);

    RetrospectivePass.Result late = retrospective.apply(graph, context, criticalPath);

    return new FloorLayout(graph, boss, criticalPath, walk.rooms(), attached.attached(), holeRooms,
        extraDoors, late.relocated(), context.loadout().unspentMandatory(), walk.reason());
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
