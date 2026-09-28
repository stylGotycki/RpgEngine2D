package net.dp.rpg.engine.tile.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

public final class TmxMapExporter {

  private final TmxMapWriter writer;

  public TmxMapExporter(TileTypeRegistry typeRegistry) {
    this.writer = new TmxMapWriter(typeRegistry);
  }

  public FileHandle export(TileMapData map, TilesetDefinition tileset, String targetPath) {
    return export(map, TmxExportSettings.of(tileset, targetPath));
  }

  public FileHandle export(TileMapData map, TmxExportSettings settings) {
    String tmx = writer.write(map, settings);
    FileHandle target = Gdx.files.local(settings.targetPath());

    target.parent().mkdirs();
    target.writeString(tmx, false, "UTF-8");

    Gdx.app.log("TmxMapExporter", "Exported %dx%d map as '%s' to %s"
        .formatted(map.width(), map.height(), settings.tileset().id(), target.path()));

    return target;
  }
}
