package net.dp.rpg.demo.interior;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;

/**
 * Single-room workbench for the interior generator: reroll a seed, cycle through shapes and room
 * types, toggle the zone overlay, and see whether the room came out of the normal pipeline or fell
 * back to the plain emergency room. Everything goes through {@link InteriorGenerator}, the same
 * entry point the whole-floor demo uses.
 */
public final class InteriorDemoApp extends ApplicationAdapter {

  private static final long FIRST_SEED = 20261004L;

  private static final float MIN_ZOOM = 0.3f;

  private static final float ZOOM_STEP = 1.15f;

  private static final float PAN_UNITS_PER_SECOND = 10f;

  private TileSystem tiles;

  private InteriorContext context;

  private InteriorGenerator generator;

  private TileMapRenderer renderer;

  private ZoneOverlay overlay;

  private TileLogger logger;

  private OrthographicCamera camera;

  private Viewport viewport;

  private int shapeIndex;

  private int typeIndex;

  private long seed = FIRST_SEED;

  private boolean showZones;

  private GeneratedRoom room;

  private TileMapData map;

  @Override
  public void create() {
    logger = new GdxTileLogger("Interior");

    tiles = new TileSystem(FIRST_SEED);
    tiles.loadTilesets(PlaceholderMotif.TILESET_PATH);

    context = PlaceholderMotif.load(tiles);
    generator = PlaceholderMotif.generator(context, tiles);

    camera = new OrthographicCamera();
    renderer = tiles.createRenderer();
    overlay = new ZoneOverlay();

    logger.log("Loaded corpus '%s': %d samples, %d warnings".formatted(PlaceholderMotif.MOTIF,
        context.corpus().samples().size(), context.corpus().warnings().size()));
    context.corpus().warnings().forEach(warning -> logger.log("  " + warning));

    generate();
  }

  private void generate() {
    RoomBlueprint blueprint = RoomPreset.blueprintOf(shapeIndex, typeIndex, seed);
    long started = System.nanoTime();

    room = generator.generate(blueprint);
    map = room.map();

    double millis = (System.nanoTime() - started) / 1e6;

    viewport = new FitViewport(map.width(), map.height(), camera);
    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
    camera.position.set(map.width() / 2f, map.height() / 2f, 0f);
    camera.zoom = 1f;

    if (room.isFallback()) {
      logger.log("FALLBACK %s/%s seed %d (%s)".formatted(blueprint.variant().id(),
          blueprint.type(), seed, room.note()));
    } else {
      logger.log("generated %s/%s seed %d: room attempts=%d time=%.2fms".formatted(
          blueprint.variant().id(), blueprint.type(), seed, room.roomAttempts(), millis));
    }

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
      overlay.render(room.canvas().zones(), camera);
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

  /** The exported map carries roomCells and roomType, so it is a valid corpus sample as it is. */
  private void exportRoom() {
    try {
      tiles.exportMap(map, PlaceholderMotif.TILESET_ID,
          "export/interior/room-%s-%d.tmx".formatted(
              room.blueprint().variant().id(), room.blueprint().seed()));

      logger.log("Exported TMX for seed " + seed);
    } catch (TileException exception) {
      logger.log("Export failed: " + exception.getMessage());
    }
  }

  private void updateTitle() {
    String status = room.isFallback() ? "FALLBACK" : "attempts=%d".formatted(room.roomAttempts());

    Gdx.graphics.setTitle(
        "Interior %s/%s seed %d - %s - zones %s - zoom %.2f".formatted(
            room.blueprint().variant().id(), room.blueprint().type(), seed, status,
            showZones ? "ON" : "off", camera.zoom)
            + " - [LEFT/RIGHT] shape [TAB] type [R] seed [Z] zones [F1/F2] debug [E] export");
  }
}
