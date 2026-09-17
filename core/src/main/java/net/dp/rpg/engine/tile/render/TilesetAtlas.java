package net.dp.rpg.engine.tile.render;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.utils.Disposable;
import lombok.Getter;
import net.dp.rpg.engine.tile.exception.InvalidTilesetException;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

public final class TilesetAtlas implements Disposable {

  @Getter
  private final TilesetDefinition definition;

  private final Texture texture;

  private final TextureRegion[] regionsByLocalId;

  @Getter
  private boolean disposed;

  private TilesetAtlas(TilesetDefinition definition, Texture texture, TextureRegion[] regionsByLocalId) {
    this.definition = definition;
    this.texture = texture;
    this.regionsByLocalId = regionsByLocalId;
  }

  public static TilesetAtlas load(TilesetDefinition definition) {
    if (definition == null) {
      throw new IllegalArgumentException("Tileset definition must not be null");
    }

    Texture texture = new Texture(Gdx.files.internal(definition.imagePath()));

    try {
      validateSize(definition, texture);

      return new TilesetAtlas(definition, texture, sliceRegions(definition, texture));
    } catch (RuntimeException exception) {
      texture.dispose();

      throw exception;
    }
  }

  public String getTilesetId() {
    return definition.id();
  }

  public TextureRegion region(int localId) {
    if (localId < 0 || localId >= regionsByLocalId.length) {
      throw new InvalidTilesetException("Local tile id %d is outside tileset '%s' holding %d tiles"
          .formatted(localId, definition.id(), regionsByLocalId.length));
    }

    return regionsByLocalId[localId];
  }

  public int size() {
    return regionsByLocalId.length;
  }

  @Override
  public void dispose() {
    if (disposed) {
      return;
    }

    texture.dispose();
    disposed = true;
  }

  private static void validateSize(TilesetDefinition definition, Texture texture) {
    int expectedWidth = definition.expectedImageWidth();
    int expectedHeight = definition.expectedImageHeight();

    if (texture.getWidth() != expectedWidth || texture.getHeight() != expectedHeight) {
      throw new InvalidTilesetException("Tileset '%s' image is %dx%d, expected %dx%d for %d tiles of %dx%d in %d columns"
          .formatted(definition.id(), texture.getWidth(), texture.getHeight(), expectedWidth, expectedHeight,
              definition.tileCount(), definition.tileWidth(), definition.tileHeight(), definition.columns()));
    }
  }

  private static TextureRegion[] sliceRegions(TilesetDefinition definition, Texture texture) {
    TextureRegion[] regions = new TextureRegion[definition.tileCount()];

    for (int localId = 0; localId < regions.length; localId++) {
      regions[localId] = new TextureRegion(texture, definition.pixelXOf(localId),
          definition.pixelYOf(localId), definition.tileWidth(), definition.tileHeight());
    }

    return regions;
  }
}
