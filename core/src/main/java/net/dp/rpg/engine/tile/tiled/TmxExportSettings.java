package net.dp.rpg.engine.tile.tiled;

import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

public record TmxExportSettings(TilesetDefinition tileset, String targetPath, Long variantSeed) {

  public TmxExportSettings {
    if (tileset == null) {
      throw new IllegalArgumentException("Target tileset must not be null");
    }

    if (targetPath == null || targetPath.isBlank()) {
      throw new IllegalArgumentException("Target path must not be blank");
    }
  }

  public static TmxExportSettings of(TilesetDefinition tileset, String targetPath) {
    return new TmxExportSettings(tileset, targetPath, null);
  }

  public static TmxExportSettings withVariants(TilesetDefinition tileset, String targetPath, long variantSeed) {
    return new TmxExportSettings(tileset, targetPath, variantSeed);
  }

  public boolean usesVariants() {
    return variantSeed != null;
  }
}
