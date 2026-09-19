package net.dp.rpg.engine.tile.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapSource;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.InvalidTiledFormatException;
import net.dp.rpg.engine.tile.tileset.TilesetManager;

public final class TmxMapSource implements TileMapSource {

  private final TmxMapParser parser;

  public TmxMapSource(TilesetManager tilesetManager, TileTypeRegistry typeRegistry) {
    this.parser = new TmxMapParser(tilesetManager, typeRegistry);
  }

  @Override
  public TileMapData load(String internalPath) {
    if (internalPath == null || internalPath.isBlank()) {
      throw new InvalidTiledFormatException("Map path must not be blank");
    }

    FileHandle file = Gdx.files.internal(internalPath);

    if (!file.exists()) {
      throw new InvalidTiledFormatException("Map file does not exist: " + internalPath);
    }

    return parser.parse(new XmlReader().parse(file), internalPath);
  }
}
