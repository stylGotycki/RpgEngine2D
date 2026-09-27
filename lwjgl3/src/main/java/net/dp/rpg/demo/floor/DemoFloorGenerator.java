package net.dp.rpg.demo.floor;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import net.dp.rpg.engine.floor.FloorBuilder;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.FloorPlan;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.RoomNode;
import net.dp.rpg.engine.floor.RoomPhase;
import net.dp.rpg.engine.floor.WalkerSettings;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class DemoFloorGenerator {

  private final GridBounds bounds;

  private final FloorBuilder builder = new FloorBuilder();

  private final WalkerSettings settings = WalkerSettings.defaults();

  public DemoFloorGenerator(int gridWidth, int gridHeight) {
    this.bounds = new GridBounds(gridWidth, gridHeight);
  }

  public List<RoomDraft> generate(long seed, int roomCount) {
    FloorLayout floor = builder.build(FloorPlan.of(roomCount), bounds, settings, seed);
    List<RoomDraft> drafts = new ArrayList<>();

    for (RoomNode room : floor.graph().rooms()) {
      RoomDraft draft = new RoomDraft(room.index(), room.shape(), room.origin(), room.depth(),
          room.phase(), room == floor.boss());

      draft.doors.addAll(room.localDoorEdges());
      drafts.add(draft);
    }

    return drafts;
  }

  public static final class RoomDraft {

    private final int id;

    private final RoomShape shape;

    private final RoomCell origin;

    private final int depth;

    private final RoomPhase phase;

    private final boolean boss;

    private final Set<RoomEdge> doors = new LinkedHashSet<>();

    private RoomDraft(int id, RoomShape shape, RoomCell origin, int depth, RoomPhase phase, boolean boss) {
      this.id = id;
      this.shape = shape;
      this.origin = origin;
      this.depth = depth;
      this.phase = phase;
      this.boss = boss;
    }

    public int getId() {
      return id;
    }

    public RoomShape getShape() {
      return shape;
    }

    public RoomCell getOrigin() {
      return origin;
    }

    public int getDepth() {
      return depth;
    }

    public RoomPhase getPhase() {
      return phase;
    }

    public boolean isBoss() {
      return boss;
    }

    public Set<RoomEdge> getDoors() {
      return doors;
    }

    public RoomCell toFloorCell(RoomCell localCell) {
      return localCell.translated(origin.x(), origin.y());
    }
  }
}
