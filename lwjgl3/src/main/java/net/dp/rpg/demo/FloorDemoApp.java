package net.dp.rpg.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.List;
import net.dp.rpg.demo.floor.DemoFloor;
import net.dp.rpg.demo.floor.DemoFloorPainter;
import net.dp.rpg.engine.floor.FloorBuilder;
import net.dp.rpg.engine.floor.FloorDebug;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.FloorPlan;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.RoomNode;
import net.dp.rpg.engine.floor.WalkerSettings;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;
import net.dp.rpg.engine.tile.room.RoomGeometry;

public final class FloorDemoApp extends ApplicationAdapter {

  private static final String[] TILESET_PATHS = {"tiles/terrain.tsx", "tiles/basement.tsx"};

  private static final int GRID_WIDTH = 25;

  private static final int GRID_HEIGHT = 25;

  private static final int ROOM_COUNT = 250;

  private static final long FIRST_SEED = 20260927L;

  private static final float MIN_ZOOM = 0.12f;

  private static final float ZOOM_STEP = 1.15f;

  private static final float PAN_CELLS_PER_SECOND = 3f;

  private final GridBounds bounds = new GridBounds(GRID_WIDTH, GRID_HEIGHT);

  private final FloorBuilder builder = new FloorBuilder();

  private TileSystem tiles;

  private DemoFloorPainter painter;

  private List<String> tilesetIds;

  private TileMapRenderer renderer;

  private TileLogger logger;

  private OrthographicCamera camera;

  private Viewport viewport;

  private DemoFloor floor;

  private long seed = FIRST_SEED;

  private int focusRoom;

  private int activeTileset;

  @Override
  public void create() {
    logger = new GdxTileLogger("Floor");

    tiles = new TileSystem(FIRST_SEED);
    tiles.loadTilesets(TILESET_PATHS);
    tilesetIds = tiles.tilesetIds();

    painter = new DemoFloorPainter(tiles.types());
    camera = new OrthographicCamera();

    viewport = new FitViewport(RoomGeometry.tileWidth(GRID_WIDTH), RoomGeometry.tileHeight(GRID_HEIGHT), camera);

    renderer = tiles.createRenderer();

    generate(seed);
  }

  private void generate(long newSeed) {
    seed = newSeed;

    FloorLayout layout = builder.build(FloorPlan.of(ROOM_COUNT), bounds, WalkerSettings.defaults(), seed);

    floor = new DemoFloor(layout, bounds, painter.paint(layout, bounds, seed), seed);
    focusRoom = 0;

    logger.log("seed %d: %s".formatted(seed, layout.summary()));
    logger.log(System.lineSeparator() + FloorDebug.render(layout.graph(), bounds));

    fitWholeFloor();
    updateTitle();
  }

  @Override
  public void render() {
    handleInput();

    ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

    viewport.apply();
    camera.update();

    renderer.render(floor.map(), camera);
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

    if (tiles != null) {
      tiles.dispose();
    }
  }

  private void handleInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      generate(seed + 1);
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
      fitWholeFloor();
      updateTitle();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
      focusOn(focusRoom + 1);
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
      focusOn(focusRoom - 1);
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.B)) {
      focusOnBoss();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
      swapTileset();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
      exportFloor();
    }

    handleZoom();
    handlePan();
    handleDebugInput();
  }

  private void handleZoom() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.EQUALS) || Gdx.input.isKeyJustPressed(Input.Keys.PLUS)) {
      camera.zoom = Math.max(MIN_ZOOM, camera.zoom / ZOOM_STEP);
      updateTitle();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
      camera.zoom = Math.min(1f, camera.zoom * ZOOM_STEP);
      updateTitle();
    }
  }

  private void handlePan() {
    float step = PAN_CELLS_PER_SECOND * RoomGeometry.CELL_WIDTH * camera.zoom * Gdx.graphics.getDeltaTime();

    if (Gdx.input.isKeyPressed(Input.Keys.A) || Gdx.input.isKeyPressed(Input.Keys.LEFT)) {
      camera.position.x -= step;
    }

    if (Gdx.input.isKeyPressed(Input.Keys.D) || Gdx.input.isKeyPressed(Input.Keys.RIGHT)) {
      camera.position.x += step;
    }

    if (Gdx.input.isKeyPressed(Input.Keys.W) || Gdx.input.isKeyPressed(Input.Keys.UP)) {
      camera.position.y += step;
    }

    if (Gdx.input.isKeyPressed(Input.Keys.S) || Gdx.input.isKeyPressed(Input.Keys.DOWN)) {
      camera.position.y -= step;
    }
  }

  private void handleDebugInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
      logger.log(tiles.debug().dump(floor.map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
      logger.log(tiles.debug().dumpWalkable(floor.map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
      logger.log(tiles.debug().describeHistogram(floor.map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
      logger.log(tiles.debug().describeCatalog());
    }
  }

  private void fitWholeFloor() {
    camera.zoom = 1f;
    camera.position.set(floor.map().width() / 2f, floor.map().height() / 2f, 0f);
  }

  private void focusOnBoss() {
    RoomNode boss = floor.layout().boss();

    if (boss != null) {
      focusOn(floor.layout().graph().rooms().indexOf(boss));
    }
  }

  private void focusOn(int index) {
    List<RoomNode> rooms = floor.layout().graph().rooms();

    focusRoom = Math.floorMod(index, rooms.size());

    RoomNode room = rooms.get(focusRoom);

    camera.zoom = Math.max(MIN_ZOOM, (float) RoomGeometry.CELL_WIDTH / floor.map().width() * 1.6f);
    camera.position.set(room.anchor().centerTileX() + 0.5f, worldY(room.anchor().centerTileY()), 0f);

    updateTitle();
  }

  /** Grid rows run downwards, world rows upwards; the renderer flips them, so focusing has to flip too. */
  private float worldY(int tileY) {
    return floor.map().height() - tileY - 0.5f;
  }

  private void swapTileset() {
    int next = (activeTileset + 1) % tilesetIds.size();

    try {
      tiles.swapTileset(tilesetIds.get(next), floor.map(), renderer);
      activeTileset = next;

      updateTitle();
    } catch (TileException exception) {
      logger.log("Swap refused: " + exception.getMessage());
    }
  }

  private void exportFloor() {
    tiles.exportMap(floor.map(), tilesetIds.get(activeTileset), "export/%s.tmx".formatted(floor.label()));

    logger.log("Exported %s".formatted(floor.label()));
  }

  private void updateTitle() {
    FloorLayout layout = floor.layout();
    RoomNode room = layout.graph().rooms().get(focusRoom);

    Gdx.graphics.setTitle(
        "Floor %d - %d rooms, critical %d, holes %d, doors %d - room %d/%d %s depth %d - zoom %.2f - tileset %s"
            .formatted(seed, layout.rooms(), layout.criticalLength(), layout.holeRooms(),
                layout.graph().links().size(), focusRoom + 1, layout.graph().size(), room.variant().id(),
                room.depth(), camera.zoom, tilesetIds.get(activeTileset))
            + " - [R] new seed [F] fit [N/P] room [B] boss [WASD] pan [+/-] zoom [T] tileset [E] export");
  }
}
