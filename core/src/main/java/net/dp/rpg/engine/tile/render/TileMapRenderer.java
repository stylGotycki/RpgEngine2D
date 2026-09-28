package net.dp.rpg.engine.tile.render;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import lombok.Getter;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileMapData;

public final class TileMapRenderer implements Disposable {

  private final SpriteBatch batch;

  private final boolean ownsBatch;

  @Getter
  private TilePalette palette;

  public TileMapRenderer(TilePalette palette) {
    this(palette, new SpriteBatch(), true);
  }

  public TileMapRenderer(TilePalette palette, SpriteBatch batch) {
    this(palette, batch, false);
  }

  private TileMapRenderer(TilePalette palette, SpriteBatch batch, boolean ownsBatch) {
    if (batch == null) {
      throw new IllegalArgumentException("Sprite batch must not be null");
    }

    this.palette = palette;
    this.batch = batch;
    this.ownsBatch = ownsBatch;
  }

  public void setPalette(TilePalette palette) {
    if (palette == null) {
      throw new IllegalArgumentException("Tile palette must not be null");
    }

    this.palette = palette;
  }

  public void render(TileMapData map, OrthographicCamera camera) {
    if (map == null || camera == null) {
      throw new IllegalArgumentException("Map and camera must not be null");
    }

    batch.setProjectionMatrix(camera.combined);
    batch.begin();

    try {
      renderLayers(map, camera);
    } finally {
      batch.end();
    }
  }

  private void renderLayers(TileMapData map, OrthographicCamera camera) {
    int height = map.height();

    float halfWidth = camera.viewportWidth * camera.zoom * 0.5f;
    float halfHeight = camera.viewportHeight * camera.zoom * 0.5f;

    int minX = clamp((int) Math.floor(camera.position.x - halfWidth), map.width());
    int maxX = clamp((int) Math.ceil(camera.position.x + halfWidth), map.width());

    int minY = clamp(height - 1 - (int) Math.ceil(camera.position.y + halfHeight), height);
    int maxY = clamp(height - 1 - (int) Math.floor(camera.position.y - halfHeight), height);

    for (TileLayer layer : map.layers()) {
      renderLayer(layer, height, minX, maxX, minY, maxY);
    }
  }

  private void renderLayer(TileLayer layer, int mapHeight, int minX, int maxX, int minY, int maxY) {
    TileGrid grid = layer.grid();

    for (int y = minY; y <= maxY; y++) {
      float worldY = mapHeight - 1 - y;

      for (int x = minX; x <= maxX; x++) {
        int runtimeId = grid.get(x, y);

        if (runtimeId == TileGrid.EMPTY) {
          continue;
        }

        TextureRegion region = palette.region(runtimeId, x, y);

        if (region == null) {
          continue;
        }

        batch.draw(region, x, worldY, 1f, 1f);
      }
    }
  }

  @Override
  public void dispose() {
    if (ownsBatch) {
      batch.dispose();
    }
  }

  private static int clamp(int value, int size) {
    return Math.max(0, Math.min(size - 1, value));
  }
}
