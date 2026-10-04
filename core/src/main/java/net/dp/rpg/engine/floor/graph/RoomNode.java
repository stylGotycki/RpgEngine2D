package net.dp.rpg.engine.floor.graph;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import lombok.Setter;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class RoomNode {

  private final int index;

  private final ShapeVariant variant;

  private final RoomCell origin;

  private final Set<RoomCell> cells;

  private final List<RoomLink> links = new ArrayList<>();

  private final Set<Integer> linkedIndices = new LinkedHashSet<>();

  @Setter
  private RoomPhase phase = RoomPhase.TRUNK;

  @Setter
  private RoomType type = RoomType.NORMAL;

  @Setter
  private String questId;

  private int depth = -1;

  RoomNode(int index, ShapeVariant variant, RoomCell origin) {
    this.index = index;
    this.variant = variant;
    this.origin = origin;
    this.cells = Collections.unmodifiableSet(toFloorCells(variant.shape(), origin));
  }

  public int index() {
    return index;
  }

  public ShapeVariant variant() {
    return variant;
  }

  public RoomShape shape() {
    return variant.shape();
  }

  public RoomCell origin() {
    return origin;
  }

  public RoomCell anchor() {
    return toFloorCell(variant.shape().topLeftCell());
  }

  public Set<RoomCell> cells() {
    return cells;
  }

  public int size() {
    return cells.size();
  }

  public boolean contains(RoomCell floorCell) {
    return cells.contains(floorCell);
  }

  public RoomCell toFloorCell(RoomCell localCell) {
    return localCell.translated(origin.x(), origin.y());
  }

  public RoomCell toLocalCell(RoomCell floorCell) {
    return floorCell.translated(-origin.x(), -origin.y());
  }

  public List<RoomEdge> outerEdges() {
    List<RoomEdge> edges = new ArrayList<>();

    for (RoomCell cell : cells) {
      for (Direction direction : Direction.values()) {
        if (!cells.contains(cell.neighbour(direction))) {
          edges.add(new RoomEdge(cell, direction));
        }
      }
    }

    return edges;
  }

  public Map<RoomEdge, DoorType> localDoors() {
    Map<RoomEdge, DoorType> doors = new LinkedHashMap<>();

    for (RoomLink link : links) {
      doors.put(toLocalEdge(link.edge()), link.doorType());
    }

    return doors;
  }

  public List<RoomLink> links() {
    return Collections.unmodifiableList(links);
  }

  public int degree() {
    return links.size();
  }

  public int neighbourCount() {
    return linkedIndices.size();
  }

  public boolean isDeadEnd() {
    return linkedIndices.size() == 1;
  }

  public boolean isLinkedTo(RoomNode other) {
    return linkedIndices.contains(other.index);
  }

  public RoomType type() {
    return type;
  }

  public String questId() {
    return questId;
  }

  public RoomPhase phase() {
    return phase;
  }

  public int depth() {
    return depth;
  }

  void setDepth(int depth) {
    this.depth = depth;
  }

  void addLink(RoomLink link) {
    links.add(link);
    linkedIndices.add(link.other(this).index);
  }

  private RoomEdge toLocalEdge(RoomEdge floorEdge) {
    if (contains(floorEdge.cell())) {
      return new RoomEdge(toLocalCell(floorEdge.cell()), floorEdge.direction());
    }

    RoomCell ownCell = floorEdge.cell().neighbour(floorEdge.direction());

    return new RoomEdge(toLocalCell(ownCell), floorEdge.direction().opposite());
  }

  @Override
  public String toString() {
    return "room[%d] %s %s at %d,%d".formatted(index, type, variant.id(), anchor().x(),
        anchor().y());
  }

  private static Set<RoomCell> toFloorCells(RoomShape shape, RoomCell origin) {
    Set<RoomCell> floorCells = new LinkedHashSet<>();

    shape.cells().forEach(cell -> floorCells.add(cell.translated(origin.x(), origin.y())));

    return floorCells;
  }
}
