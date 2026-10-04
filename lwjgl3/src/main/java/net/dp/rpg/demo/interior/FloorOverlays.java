package net.dp.rpg.demo.interior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import java.util.List;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;

final class FloorOverlays {

  private static final Color ROUTE = new Color(0.2f, 0.9f, 0.95f, 0.45f);

  private static final Color SELECTION = new Color(1f, 1f, 1f, 0.95f);

  private static final float SELECTION_WIDTH = 0.35f;

  private static final float MARKER_RADIUS = 0.45f;

  private final ShapeRenderer shapes = new ShapeRenderer();

  void route(RoomCanvas canvas, int tileOffsetX, int tileOffsetY, int mapHeight,
      OrthographicCamera camera) {
    shapes.setProjectionMatrix(camera.combined);
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(ROUTE);

    for (int y = 0; y < canvas.height(); y++) {
      for (int x = 0; x < canvas.width(); x++) {
        if (!canvas.isVoid(x, y) && canvas.isReserved(x, y)) {
          shapes.rect(tileOffsetX + x, mapHeight - 1 - (tileOffsetY + y), 1f, 1f);
        }
      }
    }

    shapes.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
  }

  void selection(RoomNode room, int minCellX, int minCellY, int mapHeight,
      OrthographicCamera camera) {
    shapes.setProjectionMatrix(camera.combined);
    shapes.begin(ShapeRenderer.ShapeType.Filled);
    shapes.setColor(SELECTION);

    for (RoomEdge edge : room.outerEdges()) {
      float left = (edge.cell().x() - minCellX) * RoomGeometry.CELL_WIDTH;
      float right = left + RoomGeometry.CELL_WIDTH;
      float top = mapHeight - (edge.cell().y() - minCellY) * RoomGeometry.CELL_HEIGHT;
      float bottom = top - RoomGeometry.CELL_HEIGHT;

      switch (edge.direction()) {
        case NORTH -> shapes.rectLine(left, top, right, top, SELECTION_WIDTH);
        case SOUTH -> shapes.rectLine(left, bottom, right, bottom, SELECTION_WIDTH);
        case WEST -> shapes.rectLine(left, bottom, left, top, SELECTION_WIDTH);
        case EAST -> shapes.rectLine(right, bottom, right, top, SELECTION_WIDTH);
      }
    }

    shapes.end();
  }

  void markers(List<TileMapObject> objects, int mapHeight, OrthographicCamera camera) {
    shapes.setProjectionMatrix(camera.combined);
    shapes.begin(ShapeRenderer.ShapeType.Line);

    for (TileMapObject object : objects) {
      if (!object.isPoint()) {
        continue;
      }

      shapes.setColor(colorOf(object.type()));
      shapes.circle(object.x(), mapHeight - object.y(), MARKER_RADIUS, 12);
    }

    shapes.end();
  }

  void dispose() {
    shapes.dispose();
  }

  private static Color colorOf(String type) {
    return switch (type) {
      case "SHOPKEEPER" -> Color.GOLD;
      case "ACCESS" -> Color.GREEN;
      case "POWER_CORE" -> Color.MAGENTA;
      case "ENEMY_SPAWN" -> Color.RED;
      case "CHEST_SPAWN" -> Color.ORANGE;
      default -> Color.CYAN;
    };
  }
}
