package net.dp.rpg.demo.interior;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.GL20;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.glutils.ShapeRenderer;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.corpus.ZoneMap;


final class ZoneOverlay {

  private static final Color WALL = new Color(0.55f, 0.55f, 0.6f, 0.35f);

  private static final Color DOOR = new Color(0.95f, 0.85f, 0.2f, 0.45f);

  private static final Color VOID = new Color(0.85f, 0.2f, 0.2f, 0.35f);

  private final ShapeRenderer shapes = new ShapeRenderer();

  void render(ZoneMap zones, OrthographicCamera camera) {
    render(zones, camera, 0, 0, zones.height());
  }

  void render(ZoneMap zones, OrthographicCamera camera, int tileOffsetX, int tileOffsetY,
      int mapHeight) {
    shapes.setProjectionMatrix(camera.combined);
    Gdx.gl.glEnable(GL20.GL_BLEND);
    shapes.begin(ShapeRenderer.ShapeType.Filled);

    for (int y = 0; y < zones.height(); y++) {
      for (int x = 0; x < zones.width(); x++) {
        Color color = colorOf(zones.zoneAt(x, y));

        if (color != null) {
          shapes.setColor(color);
          shapes.rect(tileOffsetX + x, mapHeight - 1 - (tileOffsetY + y), 1f, 1f);
        }
      }
    }

    shapes.end();
    Gdx.gl.glDisable(GL20.GL_BLEND);
  }

  void dispose() {
    shapes.dispose();
  }

  private static Color colorOf(Zone zone) {
    return switch (zone) {
      case WALL -> WALL;
      case DOOR -> DOOR;
      case VOID -> VOID;
      case INTERIOR -> null;
    };
  }
}
