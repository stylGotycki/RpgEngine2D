package net.dp.rpg.engine.floor.shape;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomShape;

public record ShapeVariant(RoomShapeDef definition, RoomShape shape, List<List<RoomCell>> holeRegions) {

  public ShapeVariant {
    holeRegions = List.copyOf(holeRegions);
  }

  public String id() {
    return definition.id();
  }

  public int size() {
    return shape.size();
  }

  public boolean hasHoles() {
    return !holeRegions.isEmpty();
  }

  public List<RoomCell> holeCellsAt(RoomCell origin) {
    List<RoomCell> cells = new ArrayList<>();

    holeRegions.forEach(region ->
        region.forEach(cell -> cells.add(cell.translated(origin.x(), origin.y()))));

    return cells;
  }
}
