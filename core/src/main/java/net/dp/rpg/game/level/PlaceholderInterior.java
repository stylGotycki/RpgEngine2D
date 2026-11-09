package net.dp.rpg.game.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.interior.prefab.DefaultInteriorTypes;
import net.dp.rpg.engine.tile.TileSystem;


public final class PlaceholderInterior {

  public static final String MOTIF = "placeholder";

  public static final String TILESET = "tiles/interior-placeholder.tsx";

  private static final String ASSET_LIST = "assets.txt";

  private static final String CORPUS_ROOT = "rooms";

  private PlaceholderInterior() {
  }

  public static InteriorContext load(TileSystem tiles) {
    return InteriorContext.load(MOTIF, assetPaths(), tiles::loadMap, tiles.types(),
        Map.of("floor.stone_any", List.of("floor.stone", "floor.stone_cracked")),
        Map.of(DoorType.NORMAL, "door.wood", DoorType.LOCKED, "door.locked"));
  }

  public static InteriorGenerator generator(InteriorContext context, TileSystem tiles) {
    return new InteriorGenerator(context, tiles.types(), DefaultInteriorTypes.catalog(),
        InteriorGenerator.Settings.DEFAULTS);
  }

  private static List<String> assetPaths() {
    FileHandle list = Gdx.files.internal(ASSET_LIST);

    if (!list.exists()) {
      // Started without Gradle (e.g. the launcher's main from the IDE): walk the folder instead,
      // which works while the working directory is assets/, but not from a jar.
      List<String> paths = new ArrayList<>();
      collect(Gdx.files.internal(CORPUS_ROOT), paths);

      if (paths.isEmpty()) {
        throw new IllegalStateException(ASSET_LIST + " is missing and " + CORPUS_ROOT
            + " cannot be listed; run the game through Gradle (lwjgl3:run), which generates it");
      }

      return paths;
    }

    return Arrays.stream(list.readString("UTF-8").split("\\R"))
        .map(String::trim)
        .filter(path -> !path.isEmpty())
        .map(path -> path.replace('\\', '/'))
        .toList();
  }

  private static void collect(FileHandle folder, List<String> paths) {
    for (FileHandle child : folder.list()) {
      if (child.isDirectory()) {
        collect(child, paths);
      } else {
        paths.add(child.path().replace('\\', '/'));
      }
    }
  }
}
