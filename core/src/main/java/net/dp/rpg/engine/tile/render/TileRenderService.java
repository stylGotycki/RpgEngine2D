package net.dp.rpg.engine.tile.render;

import com.badlogic.gdx.utils.Disposable;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetManager;

public final class TileRenderService implements Disposable {

  private final TilesetManager tilesetManager;

  private final TileTypeRegistry typeRegistry;

  private final Map<String, TilesetAtlas> atlasesByTilesetId = new LinkedHashMap<>();

  private final long variantSeed;

  public TileRenderService(TilesetManager tilesetManager, TileTypeRegistry typeRegistry,
                           long variantSeed) {
    if (tilesetManager == null) {
      throw new IllegalArgumentException("Tileset manager must not be null");
    }

    if (typeRegistry == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    this.tilesetManager = tilesetManager;
    this.typeRegistry = typeRegistry;
    this.variantSeed = variantSeed;
  }

  public TilePalette palette(String tilesetId) {
    return TilePalette.build(typeRegistry, atlas(tilesetId), variantSeed);
  }

  private TilesetAtlas atlas(String tilesetId) {
    return atlasesByTilesetId.computeIfAbsent(tilesetId,
        id -> TilesetAtlas.load(tilesetManager.require(id)));
  }

  public TilesetDefinition swap(String tilesetId, TileMapData map, TileMapRenderer renderer) {
    Collection<String> required = tilesetManager.toTypeIds(map.usedRuntimeIds());
    TilesetDefinition definition = tilesetManager.activate(tilesetId, required);

    renderer.setPalette(palette(tilesetId));

    return definition;
  }

  @Override
  public void dispose() {
    atlasesByTilesetId.values().forEach(TilesetAtlas::dispose);
    atlasesByTilesetId.clear();
  }
}
