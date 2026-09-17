package net.dp.rpg.engine.tile.render;

import com.badlogic.gdx.graphics.g2d.TextureRegion;
import java.util.List;

import lombok.Getter;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.tileset.TilesetBinding;

public final class TilePalette {

  @Getter
  private final TilesetAtlas atlas;

  @Getter
  private final long seed;

  private final TextureRegion[][] variantsByRuntimeId;

  private TilePalette(TilesetAtlas atlas, long seed, TextureRegion[][] variantsByRuntimeId) {
    this.atlas = atlas;
    this.seed = seed;
    this.variantsByRuntimeId = variantsByRuntimeId;
  }

  public static TilePalette build(TileTypeRegistry registry, TilesetAtlas atlas, long seed) {
    if (registry == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    if (atlas == null) {
      throw new IllegalArgumentException("Tileset atlas must not be null");
    }

    TilesetBinding binding = atlas.getDefinition().binding();
    TextureRegion[][] variants = new TextureRegion[registry.size()][];

    for (int runtimeId = 0; runtimeId < registry.size(); runtimeId++) {
      List<Integer> localIds = binding.variants(registry.require(runtimeId).id());

      if (localIds.isEmpty()) {
        continue;
      }

      TextureRegion[] regions = new TextureRegion[localIds.size()];

      for (int i = 0; i < regions.length; i++) {
        regions[i] = atlas.region(localIds.get(i));
      }

      variants[runtimeId] = regions;
    }

    return new TilePalette(atlas, seed, variants);
  }

  public TextureRegion region(int runtimeId, int x, int y) {
    if (runtimeId < 0 || runtimeId >= variantsByRuntimeId.length) {
      return null;
    }

    TextureRegion[] regions = variantsByRuntimeId[runtimeId];

    if (regions == null) {
      return null;
    }

    if (regions.length == 1) {
      return regions[0];
    }

    return regions[TilesetBinding.variantIndex(regions.length, x, y, seed)];
  }

  public boolean canDraw(int runtimeId) {
    return runtimeId >= 0
        && runtimeId < variantsByRuntimeId.length
        && variantsByRuntimeId[runtimeId] != null;
  }

}
