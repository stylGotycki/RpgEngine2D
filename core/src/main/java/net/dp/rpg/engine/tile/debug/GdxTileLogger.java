package net.dp.rpg.engine.tile.debug;

import com.badlogic.gdx.Gdx;

public final class GdxTileLogger implements TileLogger {

  private final String tag;

  public GdxTileLogger() {
    this("Tile");
  }

  public GdxTileLogger(String tag) {
    this.tag = tag;
  }

  @Override
  public void log(String message) {
    for (String line : message.split("\\R")) {
      if (!line.isBlank()) {
        Gdx.app.log(tag, line);
      }
    }
  }
}
