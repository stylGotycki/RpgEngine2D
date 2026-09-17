package net.dp.rpg.engine.tile.tiled;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.utils.XmlReader;
import net.dp.rpg.engine.tile.exception.InvalidTilesetException;
import net.dp.rpg.engine.tile.tileset.LoadedTileset;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetSource;

public final class TsxTilesetSource implements TilesetSource {

  private final TsxTilesetParser parser = new TsxTilesetParser();

  @Override
  public LoadedTileset load(String internalPath) {
    if (internalPath == null || internalPath.isBlank()) {
      throw InvalidTilesetException.blankField("Tileset path");
    }

    FileHandle file = Gdx.files.internal(internalPath);

    if (!file.exists()) {
      throw new InvalidTilesetException("Tileset file does not exist: " + internalPath);
    }

    LoadedTileset loaded = parser.parse(new XmlReader().parse(file), internalPath);

    validateImage(loaded.definition());

    return loaded;
  }

  private void validateImage(TilesetDefinition definition) {
    FileHandle image = Gdx.files.internal(definition.imagePath());

    if (!image.exists()) {
      throw new InvalidTilesetException("Tileset image does not exist: " + definition.imagePath());
    }
  }
}
