package net.dp.rpg.game;

import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

import java.util.*;

public final class TileFloorGenerator
{

  private static final List<RoomShape> SHAPES = List.of(
      RoomShape.single(),
      RoomShape.single(),
      RoomShape.single(),
      RoomShape.parse("##"),
      RoomShape.parse("#", "#"),
      RoomShape.parse("#.", "##"),
      RoomShape.parse("##", "##"),
      RoomShape.parse("###", ".#."),
      RoomShape.parse("###", "..#"),
      RoomShape.parse("#.#", "###", "#.#"),
      RoomShape.parse("###", "#.#", "###")
  );

  private final int gridWidth;

  private final int gridHeight;

  public TileFloorGenerator(int gridWidth, int gridHeight) {
    this.gridWidth = gridWidth;
    this.gridHeight = gridHeight;
  }

  public List<RoomDraft> generate(long seed, int roomCount) {
    Random random = new Random(seed);

    Map<RoomCell, RoomDraft> occupiedBy = new LinkedHashMap<>();
    List<RoomDraft> rooms = new ArrayList<>();

    RoomDraft first = new RoomDraft(0, RoomShape.single(),
        new RoomCell(gridWidth / 2, gridHeight / 2));

    place(first, occupiedBy, rooms);

    int attempts = 0;

    while (rooms.size() < roomCount && attempts++ < roomCount * 50) {
      tryGrow(random, occupiedBy, rooms);
    }

    return rooms;
  }

  private void tryGrow(Random random, Map<RoomCell, RoomDraft> occupiedBy, List<RoomDraft> rooms) {
    RoomDraft source = rooms.get(random.nextInt(rooms.size()));
    List<RoomEdge> edges = source.shape.outerEdges();
    RoomEdge edge = edges.get(random.nextInt(edges.size()));

    RoomCell fromCell = source.toFloorCell(edge.cell());
    RoomCell target = fromCell.neighbour(edge.direction());

    if (occupiedBy.containsKey(target) || !isInsideGrid(target)) {
      return;
    }

    RoomShape shape = SHAPES.get(random.nextInt(SHAPES.size()));

    for (RoomCell anchor : shuffled(shape.cells(), random)) {
      RoomCell origin = new RoomCell(target.x() - anchor.x(), target.y() - anchor.y());

      if (!fits(shape, origin, occupiedBy)) {
        continue;
      }

      RoomDraft room = new RoomDraft(rooms.size(), shape, origin);

      place(room, occupiedBy, rooms);

      source.doors.add(edge);
      room.doors.add(new RoomEdge(anchor, edge.direction().opposite()));

      return;
    }
  }

  private boolean fits(RoomShape shape, RoomCell origin, Map<RoomCell, RoomDraft> occupiedBy) {
    for (RoomCell cell : shape.cells()) {
      RoomCell floorCell = cell.translated(origin.x(), origin.y());

      if (occupiedBy.containsKey(floorCell) || !isInsideGrid(floorCell)) {
        return false;
      }
    }

    return true;
  }

  private void place(RoomDraft room, Map<RoomCell, RoomDraft> occupiedBy, List<RoomDraft> rooms) {
    room.shape.cells().forEach(cell -> occupiedBy.put(room.toFloorCell(cell), room));
    rooms.add(room);
  }

  private boolean isInsideGrid(RoomCell cell) {
    return cell.x() >= 0 && cell.x() < gridWidth && cell.y() >= 0 && cell.y() < gridHeight;
  }

  private static List<RoomCell> shuffled(Set<RoomCell> cells, Random random) {
    List<RoomCell> shuffled = new ArrayList<>(cells);

    java.util.Collections.shuffle(shuffled, random);

    return shuffled;
  }

  public static final class RoomDraft {

    final int id;

    final RoomShape shape;

    final RoomCell origin;

    final Set<RoomEdge> doors = new LinkedHashSet<>();

    RoomDraft(int id, RoomShape shape, RoomCell origin) {
      this.id = id;
      this.shape = shape;
      this.origin = origin;
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

    public Set<RoomEdge> getDoors() {
      return doors;
    }

    RoomCell toFloorCell(RoomCell localCell) {
      return localCell.translated(origin.x(), origin.y());
    }
  }
}
