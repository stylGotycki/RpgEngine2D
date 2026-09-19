package net.dp.rpg.engine.tile;

import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import net.dp.rpg.engine.tile.debug.TileDebug;
import net.dp.rpg.engine.tile.render.TileMapRenderer;
import net.dp.rpg.engine.tile.render.TilePalette;
import net.dp.rpg.engine.tile.render.TileRenderService;
import net.dp.rpg.engine.tile.tiled.TmxExportSettings;
import net.dp.rpg.engine.tile.tiled.TmxMapExporter;
import net.dp.rpg.engine.tile.tiled.TmxMapSource;
import net.dp.rpg.engine.tile.tiled.TsxTilesetSource;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetManager;
import net.dp.rpg.engine.tile.tileset.TilesetSource;

import java.util.Arrays;
import java.util.List;

public final class TileSystem {

  private final TileTypeRegistry typeRegistry;

  private final TilesetManager tilesetManager;

  private final TileMapSource mapSource;

  private final TmxMapExporter exporter;

  private final TileRenderService renderService;

  private final TileDebug debug;

  public TileSystem(long variantSeed) {
    this(new TsxTilesetSource(), variantSeed);
  }

  public TileSystem(TilesetSource tilesetSource, long variantSeed) {
    this.typeRegistry = new TileTypeRegistry();
    this.tilesetManager = new TilesetManager(tilesetSource, typeRegistry);
    this.mapSource = new TmxMapSource(tilesetManager, typeRegistry);
    this.exporter = new TmxMapExporter(typeRegistry);
    this.renderService = new TileRenderService(tilesetManager, typeRegistry, variantSeed);
    this.debug = new TileDebug(typeRegistry, tilesetManager);
  }

  public List<TilesetDefinition> loadTilesets(String... internalPaths) {
    return Arrays.stream(internalPaths)
        .map(tilesetManager::load)
        .toList();
  }

  public TilesetDefinition loadTileset(String internalPath) {
    return tilesetManager.load(internalPath);
  }

  public TileMapData loadMap(String internalPath) {
    return mapSource.load(internalPath);
  }

  public FileHandle exportMap(TileMapData map, String tilesetId, String targetPath) {
    return exporter.export(map, tilesetManager.require(tilesetId), targetPath);
  }

  public FileHandle exportMap(TileMapData map, TmxExportSettings settings) {
    return exporter.export(map, settings);
  }

  public TileMapRenderer createRenderer() {
    return new TileMapRenderer(activePalette());
  }

  public TileMapRenderer createRenderer(SpriteBatch batch) {
    return new TileMapRenderer(activePalette(), batch);
  }

  public TilesetDefinition swapTileset(String tilesetId, TileMapData map,
                                       TileMapRenderer renderer) {
    return renderService.swap(tilesetId, map, renderer);
  }

  public List<String> tilesetIds() {
    return tilesetManager.getLoaded().stream()
        .map(TilesetDefinition::id)
        .toList();
  }

  public String activeTilesetId() {
    return tilesetManager.getActiveTilesetId();
  }

  public TileTypeRegistry types() {
    return typeRegistry;
  }

  public TilesetManager tilesets() {
    return tilesetManager;
  }

  public TileRenderService rendering() {
    return renderService;
  }

  public TileDebug debug() {
    return debug;
  }

  public void dispose() {
    renderService.dispose();
  }

  private TilePalette activePalette() {
    return renderService.palette(tilesetManager.requireActive().id());
  }
}
