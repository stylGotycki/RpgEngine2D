package net.dp.rpg.demo.interior;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.interior.prefab.DefaultInteriorTypes;
import net.dp.rpg.engine.tile.TileSystem;

/**
 * How the demos load the placeholder motif: where its tileset and corpus live, which tile types
 * count as interchangeable, and which class stands for each kind of door. Both interior demos
 * build from this one place, so the single-room workbench and the whole-floor view can never
 * disagree about what a motif is.
 */
final class PlaceholderMotif {

  static final String MOTIF = "placeholder";

  static final String TILESET_PATH = "tiles/interior-placeholder.tsx";

  static final String TILESET_ID = "interior-placeholder";

  private PlaceholderMotif() {
  }

  static InteriorContext load(TileSystem tiles) {
    return InteriorContext.load(MOTIF, discoverAssets(), tiles::loadMap, tiles.types(),
        Map.of("floor.stone_any", List.of("floor.stone", "floor.stone_cracked")),
        Map.of(DoorType.NORMAL, "door.wood", DoorType.LOCKED, "door.locked"));
  }

  static InteriorGenerator generator(InteriorContext context, TileSystem tiles) {
    return new InteriorGenerator(context, tiles.types(), DefaultInteriorTypes.catalog(),
        InteriorGenerator.Settings.DEFAULTS);
  }

  /**
   * Walks the process's working directory for asset paths, the same thing {@code assets.txt}
   * lists. This only finds anything when the working directory is actually {@code assets/}: the
   * Gradle tasks in {@code lwjgl3/build.gradle} set it explicitly, but an IDE's own "run this main
   * method" action usually does not, which silently leaves the corpus with zero samples instead of
   * pointing at the real cause.
   */
  private static List<String> discoverAssets() {
    Path root = Path.of(".");

    try (Stream<Path> walk = Files.walk(root)) {
      List<String> paths = walk.filter(Files::isRegularFile)
          .map(path -> root.relativize(path).toString())
          .collect(Collectors.toList());

      if (paths.stream().noneMatch(path -> path.equals("assets.txt"))) {
        String cwd = root.toAbsolutePath().normalize().toString();

        throw new IllegalStateException(
            ("Working directory is '%s', which has no assets.txt; the room corpus would load zero "
                + "samples. Run 'runInteriorDemo' or 'runFloorInteriorDemo' through Gradle (they "
                + "set the working directory to assets/) instead of starting a main() directly "
                + "from the IDE.").formatted(cwd));
      }

      return paths;
    } catch (IOException exception) {
      throw new UncheckedIOException(exception);
    }
  }
}
