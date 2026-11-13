package net.dp.rpg.game.minimap;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.events.EnterRoomEvent;

/**
 * The minimap in the corner of the screen: the whole floor scaled into a fixed box, with only the
 * rooms {@link MinimapModel} reveals. Visited rooms are light grey, rooms seen through a door dark
 * grey, the player's room has a green frame and special rooms keep their icon in both states.
 *
 * <p>Drawn with one white atlas region tinted per rectangle, so it needs no ShapeRenderer and goes
 * into the stage's batch with the rest of the GUI. Stage and floor are both y-up, so a floor cell
 * maps to the widget without a flip.
 */
public class MinimapWidget extends Actor implements GameEventListener {

  private static final Color BACKGROUND = new Color(0f, 0f, 0f, 0.55f);

  private static final Color VISITED = new Color(0.72f, 0.72f, 0.72f, 1f);

  private static final Color UNVISITED = new Color(0.30f, 0.30f, 0.32f, 1f);

  private static final Color CURRENT = new Color(0.30f, 0.85f, 0.35f, 1f);

  /** Gap between rooms, in pixels on each side of a room wall. */
  private static final int GAP = 1;

  private static final int PADDING = 6;

  private static final int ICON_SIZE = 10;

  private final MinimapModel model;

  private final TextureRegion pixel;

  private final Map<RoomType, TextureRegion> icons = new EnumMap<>(RoomType.class);

  private final GridBounds extent;

  private final int cellWidth;

  private final int cellHeight;

  private final Color tint = new Color();

  private final TextureRegion questIcon;

  private Supplier<Set<Integer>> questRooms = Set::of;

  /**
   * @param maxWidth widest the map may get on screen, in pixels
   * @param maxHeight highest the map may get on screen, in pixels
   */
  public MinimapWidget(FloorLayout layout, TextureAtlas atlas, int maxWidth, int maxHeight) {
    this.model = new MinimapModel(layout);
    this.pixel = atlas.findRegion(MinimapIcons.PIXEL);
    this.extent = layout.extent();
    this.questIcon = atlas.findRegion(MinimapIcons.QUEST);

    if (pixel == null) {
      throw new IllegalStateException(MinimapIcons.PIXEL + " is missing from the atlas; repack "
          + "textures-raw (texturePackerApplication)");
    }

    MinimapIcons.all().forEach((type, region) -> {
      TextureRegion icon = atlas.findRegion(region);

      if (icon != null) {
        icons.put(type, icon);
      }
    });

    // Cells keep the 19:13 shape of a room cell; the floor is scaled to fit the box.
    float scale = Math.min(
        (maxWidth - 2f * PADDING) / (extent.width() * RoomGeometry.CELL_WIDTH),
        (maxHeight - 2f * PADDING) / (extent.height() * RoomGeometry.CELL_HEIGHT));
    cellWidth = Math.max(8, Math.round(scale * RoomGeometry.CELL_WIDTH));
    cellHeight = Math.max(6, Math.round(scale * RoomGeometry.CELL_HEIGHT));

    setSize(extent.width() * cellWidth + 2 * PADDING, extent.height() * cellHeight + 2 * PADDING);
  }

  public void setQuestRooms(Supplier<Set<Integer>> rooms) {
    this.questRooms = rooms;
  }

  public MinimapModel model() {
    return model;
  }

  @Override
  public void onEvent(GameEvent event) {
    // The event bus outlives scenes, so a minimap of an earlier floor may still be listening.
    if (event instanceof EnterRoomEvent roomEvent && model.owns(roomEvent.room.node())) {
      model.enter(roomEvent.room.node());
    }
  }

  @Override
  public void draw(Batch batch, float parentAlpha) {
    Color previous = batch.getColor().cpy();

    fill(batch, parentAlpha, BACKGROUND, getX(), getY(), getWidth(), getHeight());

    for (RoomLink door : model.knownDoors()) {
      drawDoor(batch, parentAlpha, door);
    }

    for (RoomNode room : model.revealedRooms()) {
      drawRoom(batch, parentAlpha, room, model.isVisited(room) ? VISITED : UNVISITED);
    }

    drawFrame(batch, parentAlpha, model.current());

    Set<Integer> marked = questRooms.get();
    for (RoomNode room : model.revealedRooms()) {
      drawIcon(batch, parentAlpha, room);
      if (marked.contains(room.index())) {
        drawQuestMark(batch, parentAlpha, room);
      }
    }

    batch.setColor(previous);
  }

  private void drawRoom(Batch batch, float alpha, RoomNode room, Color color) {
    for (RoomCell cell : room.cells()) {
      float x = cellX(cell);
      float y = cellY(cell);
      float left = isWall(room, cell, Direction.WEST) ? GAP : 0;
      float right = isWall(room, cell, Direction.EAST) ? GAP : 0;
      float bottom = isWall(room, cell, Direction.SOUTH) ? GAP : 0;
      float top = isWall(room, cell, Direction.NORTH) ? GAP : 0;

      fill(batch, alpha, color, x + left, y + bottom, cellWidth - left - right,
          cellHeight - bottom - top);
    }
  }

  /** Green frame along the outer walls of the room, 2 px inside the gap. */
  private void drawFrame(Batch batch, float alpha, RoomNode room) {
    float thickness = 2;

    for (RoomCell cell : room.cells()) {
      float x = cellX(cell);
      float y = cellY(cell);

      if (isWall(room, cell, Direction.WEST)) {
        fill(batch, alpha, CURRENT, x + GAP, y, thickness, cellHeight);
      }
      if (isWall(room, cell, Direction.EAST)) {
        fill(batch, alpha, CURRENT, x + cellWidth - GAP - thickness, y, thickness, cellHeight);
      }
      if (isWall(room, cell, Direction.SOUTH)) {
        fill(batch, alpha, CURRENT, x, y + GAP, cellWidth, thickness);
      }
      if (isWall(room, cell, Direction.NORTH)) {
        fill(batch, alpha, CURRENT, x, y + cellHeight - GAP - thickness, cellWidth, thickness);
      }
    }
  }

  /** A short bar across the gap between two rooms, in the middle of the cell side. */
  private void drawDoor(Batch batch, float alpha, RoomLink door) {
    RoomCell cell = door.edge().cell();
    Direction side = door.edge().direction();
    float x = cellX(cell);
    float y = cellY(cell);
    float across = 2 * GAP + 2;
    float along = Math.max(2, Math.round((side.isVertical() ? cellWidth : cellHeight) / 4f));

    switch (side) {
      case EAST -> fill(batch, alpha, VISITED, x + cellWidth - across / 2,
          y + (cellHeight - along) / 2, across, along);
      case WEST -> fill(batch, alpha, VISITED, x - across / 2, y + (cellHeight - along) / 2,
          across, along);
      case NORTH -> fill(batch, alpha, VISITED, x + (cellWidth - along) / 2,
          y + cellHeight - across / 2, along, across);
      case SOUTH -> fill(batch, alpha, VISITED, x + (cellWidth - along) / 2, y - across / 2,
          along, across);
      default -> {
      }
    }
  }

  private void drawIcon(Batch batch, float alpha, RoomNode room) {
    TextureRegion icon = icons.get(room.type());

    if (icon == null) {
      return;
    }

    RoomCell cell = iconCell(room);
    // Whole multiples of the 10 px art keep the pixels sharp.
    int size = ICON_SIZE * Math.max(1, Math.min(cellWidth, cellHeight) * 3 / 4 / ICON_SIZE);
    float x = cellX(cell) + (cellWidth - size) / 2f;
    float y = cellY(cell) + (cellHeight - size) / 2f;

    batch.setColor(1f, 1f, 1f, alpha);
    batch.draw(icon, Math.round(x), Math.round(y), size, size);
  }

  /** The room cell closest to the middle of its cells, so an L-shaped room has its icon inside. */
  // In the top right corner of the icon's cell, so it does not hide the room's own icon.
  private void drawQuestMark(Batch batch, float alpha, RoomNode room) {
    if (questIcon == null) {
      return;
    }

    RoomCell cell = iconCell(room);
    int size = ICON_SIZE * Math.max(1, Math.min(cellWidth, cellHeight) / 2 / ICON_SIZE);

    batch.setColor(1f, 1f, 1f, alpha);
    batch.draw(questIcon, cellX(cell) + cellWidth - size - GAP - 1,
        cellY(cell) + cellHeight - size - GAP - 1, size, size);
  }

  static RoomCell iconCell(RoomNode room) {
    double centerX = room.cells().stream().mapToInt(RoomCell::x).average().orElse(0);
    double centerY = room.cells().stream().mapToInt(RoomCell::y).average().orElse(0);
    RoomCell best = room.anchor();
    double bestDistance = Double.MAX_VALUE;

    for (RoomCell cell : room.cells()) {
      double distance = Math.pow(cell.x() - centerX, 2) + Math.pow(cell.y() - centerY, 2);

      if (distance < bestDistance - 1e-9
          || (Math.abs(distance - bestDistance) < 1e-9 && compare(cell, best) < 0)) {
        best = cell;
        bestDistance = distance;
      }
    }

    return best;
  }

  /** Bottom row first, then left to right: the same cell every frame for ties. */
  private static int compare(RoomCell first, RoomCell second) {
    return first.y() != second.y() ? Integer.compare(first.y(), second.y())
        : Integer.compare(first.x(), second.x());
  }

  private static boolean isWall(RoomNode room, RoomCell cell, Direction side) {
    return !room.contains(cell.neighbour(side));
  }

  private float cellX(RoomCell cell) {
    return getX() + PADDING + cell.x() * cellWidth;
  }

  private float cellY(RoomCell cell) {
    return getY() + PADDING + cell.y() * cellHeight;
  }

  private void fill(Batch batch, float alpha, Color color, float x, float y, float width,
      float height) {
    tint.set(color);
    tint.a *= alpha;
    batch.setColor(tint);
    batch.draw(pixel, x, y, width, height);
  }
}
