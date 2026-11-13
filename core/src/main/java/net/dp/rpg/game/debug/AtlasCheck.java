package net.dp.rpg.game.debug;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import java.util.ArrayList;
import java.util.List;

// textures.atlas and textures.png must come from the same packing: an atlas that knows a region
// over an older png draws that region as empty, with no error anywhere.
public final class AtlasCheck {

  private AtlasCheck() {
  }

  public static List<String> emptyRegions(String atlasPath, String prefix) {
    FileHandle atlasFile = Gdx.files.internal(atlasPath);
    TextureAtlas.TextureAtlasData data = new TextureAtlas.TextureAtlasData(atlasFile,
        atlasFile.parent(), false);
    List<String> empty = new ArrayList<>();

    for (TextureAtlas.TextureAtlasData.Page page : data.getPages()) {
      Pixmap pixels = new Pixmap(page.textureFile);

      for (TextureAtlas.TextureAtlasData.Region region : data.getRegions()) {
        if (region.page == page && region.name.startsWith(prefix)
            && !hasInk(pixels, region.left, region.top, region.width, region.height)) {
          empty.add(region.name);
        }
      }
      pixels.dispose();
    }

    return empty;
  }

  private static boolean hasInk(Pixmap pixels, int x, int y, int width, int height) {
    for (int dy = 0; dy < height; dy++) {
      for (int dx = 0; dx < width; dx++) {
        if ((pixels.getPixel(x + dx, y + dy) & 0xFF) != 0) {
          return true;
        }
      }
    }

    return false;
  }

  public static void report(String atlasPath) {
    List<String> empty = emptyRegions(atlasPath, "puzzle/");

    if (empty.isEmpty()) {
      Gdx.app.log("Atlas", "puzzle regions OK");
    } else {
      Gdx.app.error("Atlas", "Empty regions " + empty + ": " + atlasPath + " and its png come "
          + "from different packings; copy both from the same commit");
    }
  }
}
