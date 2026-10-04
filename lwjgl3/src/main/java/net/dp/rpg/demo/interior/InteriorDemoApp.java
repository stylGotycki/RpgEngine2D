package net.dp.rpg.demo.interior;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.detail.LearnedDetailPass;
import net.dp.rpg.engine.interior.pass.PrefabPhase;
import net.dp.rpg.engine.interior.pass.SkeletonPass;
import net.dp.rpg.engine.interior.prefab.DefaultInteriorTypes;
import net.dp.rpg.engine.interior.prefab.PrefabPlacer;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;

public final class InteriorDemoApp extends ApplicationAdapter {

  private static final String MOTIF = "placeholder";

  private static final String TILESET_PATH = "tiles/interior-placeholder.tsx";

  private static final long FIRST_SEED = 20261004L;

  private static final int MAX_ATTEMPTS = 30;

  private static final float MIN_ZOOM = 0.3f;

  private static final float ZOOM_STEP = 1.15f;

  private static final float PAN_UNITS_PER_SECOND = 10f;

  private TileSystem tiles;

  private InteriorContext context;

  private SkeletonPass skeletonPass;

  private LearnedDetailPass detailPass;

  private TileMapRenderer renderer;

  private ZoneOverlay overlay;

  private TileLogger logger;

  private OrthographicCamera camera;

  private Viewport viewport;

  private int shapeIndex;

  private int typeIndex;

  private long seed = FIRST_SEED;

  private boolean showZones;

  private SkeletonPass.Result result;

  private TileMapData map;

  @Override
  public void create() {
    logger = new GdxTileLogger("Interior");

    tiles = new TileSystem(FIRST_SEED);
    tiles.loadTilesets(TILESET_PATH);

    context = InteriorContext.load(MOTIF, discoverAssets(), tiles::loadMap, tiles.types(),
        Map.of("floor.stone_any", List.of("floor.stone", "floor.stone_cracked")),
        Map.of(DoorType.NORMAL, "door.wood", DoorType.LOCKED, "door.locked"));
    PrefabPlacer placer = new PrefabPlacer(context.classes());
    PrefabPhase prefabPhase =
        new PrefabPhase(DefaultInteriorTypes.catalog(), context.prefabs(), placer);

    skeletonPass = new SkeletonPass(context.model(), context.doors(), context.walkable(),
        prefabPhase, MAX_ATTEMPTS);
    detailPass = new LearnedDetailPass(context.details(),
        runtimeId -> !tiles.types().require(runtimeId).walkable());

    camera = new OrthographicCamera();
    renderer = tiles.createRenderer();
    overlay = new ZoneOverlay();

    logger.log("Loaded corpus '%s': %d samples, %d warnings".formatted(MOTIF,
        context.corpus().samples().size(), context.corpus().warnings().size()));
    context.corpus().warnings().forEach(warning -> logger.log("  " + warning));

    generate();
  }

  private List<String> discoverAssets() {
    Path root = Path.of(".");

    try (Stream<Path> walk = Files.walk(root)) {
      List<String> paths = walk.filter(Files::isRegularFile)
          .map(path -> root.relativize(path).toString())
          .collect(Collectors.toList());

      if (paths.stream().noneMatch(path -> path.equals("assets.txt"))) {
        String cwd = root.toAbsolutePath().normalize().toString();

        throw new IllegalStateException(
            ("Working directory is '%s', which has no assets.txt; the room corpus would load zero "
                + "samples. Run the 'runInteriorDemo' Gradle task (it sets the working directory "
                + "to assets/) instead of starting this class's main() directly from the IDE.")
                .formatted(cwd));
      }

      return paths;
    } catch (java.io.IOException exception) {
      throw new java.io.UncheckedIOException(exception);
    }
  }

  private void generate() {
    RoomBlueprint blueprint = RoomPreset.blueprintOf(shapeIndex, typeIndex, seed);
    long started = System.nanoTime();

    result = skeletonPass.generate(blueprint);

    double millis = (System.nanoTime() - started) / 1e6;

    if (!result.isSolved()) {
      logger.log("FAILED to solve %s/%s seed %d after %d attempts (%s)".formatted(
          RoomPreset.SHAPES.get(Math.floorMod(shapeIndex, RoomPreset.SHAPES.size())).id(),
          RoomPreset.TYPES.get(Math.floorMod(typeIndex, RoomPreset.TYPES.size())), seed,
          result.ground().attempts(), result.ground().status()));
      updateTitle();

      return;
    }

    detailPass.apply(result.canvas(), result.ground().state(), new Random(seed * 31 + 7));
    map = result.canvas().toMapData(result.ground().state(), new Random(seed * 17 + 3));

    viewport = new FitViewport(map.width(), map.height(), camera);
    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.position.set(map.width() / 2f, map.height() / 2f, 0f);
    camera.zoom = 1f;

    logger.log("solved %s/%s seed %d: attempts=%d observations=%d time=%.2fms".formatted(
        blueprint.variant().id(), blueprint.type(), seed, result.ground().attempts(),
        result.ground().observations(), millis));

    updateTitle();
  }

  @Override
  public void render() {
    handleInput();

    ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

    if (map == null) {
      return;
    }

    viewport.apply();
    camera.update();

    renderer.render(map, camera);

    if (showZones) {
      overlay.render(result.canvas().zones(), camera);
    }
  }

  @Override
  public void resize(int width, int height) {
    if (viewport != null) {
      viewport.update(width, height);
    }
  }

  @Override
  public void dispose() {
    if (renderer != null) {
      renderer.dispose();
    }

    if (overlay != null) {
      overlay.dispose();
    }

    if (tiles != null) {
      tiles.dispose();
    }
  }

  private void handleInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      seed++;
      generate();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.RIGHT)) {
      shapeIndex++;
      generate();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.LEFT)) {
      shapeIndex--;
      generate();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
      typeIndex++;
      generate();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
      showZones = !showZones;
      updateTitle();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F1) && map != null) {
      logger.log(tiles.debug().dump(map));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F2) && map != null) {
      logger.log(tiles.debug().describeHistogram(map));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.E) && map != null) {
      exportRoom();
    }

    handleZoom();
    handlePan();
  }

  private void handleZoom() {
    boolean zoomIn = Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)
        || Gdx.input.isKeyJustPressed(Input.Keys.PLUS);

    if (zoomIn) {
      camera.zoom = Math.max(MIN_ZOOM, camera.zoom / ZOOM_STEP);
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
      camera.zoom = Math.min(2f, camera.zoom * ZOOM_STEP);
    }
  }

  private void handlePan() {
    float step = PAN_UNITS_PER_SECOND * camera.zoom * Gdx.graphics.getDeltaTime();

    if (Gdx.input.isKeyPressed(Input.Keys.A)) {
      camera.position.x -= step;
    }

    if (Gdx.input.isKeyPressed(Input.Keys.D)) {
      camera.position.x += step;
    }

    if (Gdx.input.isKeyPressed(Input.Keys.W)) {
      camera.position.y += step;
    }

    if (Gdx.input.isKeyPressed(Input.Keys.S)) {
      camera.position.y -= step;
    }
  }

  private void exportRoom() {
    try {
      tiles.exportMap(map, "interior-placeholder",
          "export/room-%s-%d.tmx".formatted(RoomPreset.SHAPES.get(
              Math.floorMod(shapeIndex, RoomPreset.SHAPES.size())).id(), seed));

      logger.log("Exported TMX for seed " + seed);
    } catch (TileException exception) {
      logger.log("Export failed: " + exception.getMessage());
    }
  }

  private void updateTitle() {
    int shapePosition = Math.floorMod(shapeIndex, RoomPreset.SHAPES.size());
    int typePosition = Math.floorMod(typeIndex, RoomPreset.TYPES.size());
    String shapeId = RoomPreset.SHAPES.get(shapePosition).id();
    String type = RoomPreset.TYPES.get(typePosition).toString();
    String status = result != null && result.isSolved()
        ? "attempts=%d".formatted(result.ground().attempts())
        : "UNSOLVED";

    Gdx.graphics.setTitle(
        "Interior %s/%s seed %d - %s - zones %s - zoom %.2f".formatted(shapeId, type, seed,
            status, showZones ? "ON" : "off", camera.zoom)
            + " - [LEFT/RIGHT] shape [TAB] type [R] seed [Z] zones [F1/F2] debug [E] export");
  }
}
