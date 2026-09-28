package net.dp.rpg.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.files.FileHandle;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.List;
import net.dp.rpg.demo.floor.DemoFloorPainter;
import net.dp.rpg.demo.floor.FloorReplay;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;
import net.dp.rpg.engine.floor.FloorDebug;
import net.dp.rpg.engine.floor.FloorGenerator;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.io.FloorDocument;
import net.dp.rpg.engine.floor.io.FloorDocuments;
import net.dp.rpg.engine.floor.io.FloorJson;
import net.dp.rpg.engine.floor.io.FloorRestore;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomGeometry;

public final class FloorDemoApp extends ApplicationAdapter {

  private static final String[] TILESET_PATHS = {"tiles/rooms.tsx"};

  private static final long FIRST_SEED = 20260928L;

  private static final float[] STEP_SECONDS = {1.0f, 0.5f, 0.2f, 0.05f};

  private static final float MIN_ZOOM = 0.12f;

  private static final float ZOOM_STEP = 1.15f;

  private static final float PAN_CELLS_PER_SECOND = 3f;

  private final FloorGenerator generator = new FloorGenerator();

  private TileSystem tiles;

  private DemoFloorPainter painter;

  private List<String> tilesetIds;

  private TileMapRenderer renderer;

  private TileLogger logger;

  private OrthographicCamera camera;

  private Viewport viewport;

  private FloorArchetype archetype = FloorArchetypes.CAVES;

  private FloorLayout layout;

  private GridBounds bounds;

  private FloorReplay replay;

  private TileMapData map;

  private long seed = FIRST_SEED;

  private int speed = 1;

  private int focusRoom;

  private float sinceStep;

  private boolean playing = true;

  @Override
  public void create() {
    logger = new GdxTileLogger("Floor");

    tiles = new TileSystem(FIRST_SEED);
    tiles.loadTilesets(TILESET_PATHS);
    tilesetIds = tiles.tilesetIds();

    painter = new DemoFloorPainter(tiles.types());
    camera = new OrthographicCamera();
    renderer = tiles.createRenderer();

    generate(seed);
  }

  private void generate(long newSeed) {
    seed = newSeed;
    layout = generator.generate(archetype, seed);
    bounds = boundsOf(layout);
    replay = new FloorReplay(layout);
    focusRoom = 0;
    sinceStep = 0f;
    playing = true;

    viewport = new FitViewport(RoomGeometry.tileWidth(bounds.width()),
        RoomGeometry.tileHeight(bounds.height()), camera);
    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    logger.log("%s seed %d: %s".formatted(archetype.id(), seed, layout.summary()));
    logger.log(System.lineSeparator() + FloorDebug.render(layout.graph(), bounds));

    repaint();
    fitWholeFloor();
  }

  private GridBounds boundsOf(FloorLayout floor) {
    int maxX = 0;
    int maxY = 0;

    for (RoomNode room : floor.graph().rooms()) {
      for (RoomCell cell : room.cells()) {
        maxX = Math.max(maxX, cell.x());
        maxY = Math.max(maxY, cell.y());
      }
    }

    return new GridBounds(maxX + 1, maxY + 1);
  }

  private void repaint() {
    map = painter.paint(layout, bounds, replay.reveal());

    updateTitle();
  }

  @Override
  public void render() {
    handleInput();
    advanceReplay();

    ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

    viewport.apply();
    camera.update();

    renderer.render(map, camera);
  }

  private void advanceReplay() {
    if (!playing || replay.finished()) {
      return;
    }

    sinceStep += Gdx.graphics.getDeltaTime();

    if (sinceStep < STEP_SECONDS[speed]) {
      return;
    }

    sinceStep = 0f;
    replay.advance();
    repaint();
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

    if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
      switchArchetype();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.SPACE)) {
      playing = !playing;
      updateTitle();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.PERIOD)) {
      playing = false;
      replay.advance();
      repaint();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.ENTER)) {
      replay.finish();
      repaint();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.BACKSPACE)) {
      replay.restart();
      playing = true;
      repaint();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.O)) {
      saveFloor();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
      loadFloor();
    }

    handleSpeedInput();
    handleViewInput();
    handleZoom();
    handlePan();
    handleDebugInput();
  }

  private void saveFloor() {
    FloorDocument document = FloorDocuments.of(layout, archetype, bounds);
    FileHandle file = Gdx.files.local(fileName());

    file.writeString(FloorJson.write(document), false);

    logger.log("Saved %s (%d rooms, %d doors)".formatted(fileName(), document.rooms().size(),
        document.doors().size()));
  }

  private void loadFloor() {
    FileHandle file = Gdx.files.local(fileName());

    if (!file.exists()) {
      logger.log("No saved floor at " + fileName());

      return;
    }

    try {
      FloorRestore.Result restored = FloorRestore.read(FloorJson.read(file.readString()));

      logger.log("Loaded %s: rooms %d of %d, doors %d of %d, quests %d of %d".formatted(
          fileName(), restored.graph().size(), layout.rooms(),
          restored.graph().links().size(), layout.graph().links().size(),
          restored.quests().size(), layout.quests().size()));

      restored.warnings().forEach(warning -> logger.log("  warning: " + warning));
    } catch (RuntimeException exception) {
      logger.log("Load failed: " + exception.getMessage());
    }
  }

  private String fileName() {
    return "export/floor-%s-%d.json".formatted(archetype.id(), seed);
  }

  private void switchArchetype() {
    int next = (FloorArchetypes.ALL.indexOf(archetype) + 1) % FloorArchetypes.ALL.size();

    archetype = FloorArchetypes.ALL.get(next);

    generate(seed);
  }

  private void handleSpeedInput() {
    int[] keys = {Input.Keys.NUM_1, Input.Keys.NUM_2, Input.Keys.NUM_3, Input.Keys.NUM_4};

    for (int index = 0; index < keys.length; index++) {
      if (Gdx.input.isKeyJustPressed(keys[index])) {
        speed = index;
        updateTitle();
      }
    }
  }

  private void handleViewInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.F)) {
      fitWholeFloor();
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

    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
      exportFloor();
    }
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
      logger.log(tiles.debug().dump(map));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
      logger.log(tiles.debug().dumpWalkable(map));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
      logger.log(tiles.debug().describeHistogram(map));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
      logger.log(tiles.debug().describeCatalog());
    }
  }

  private void fitWholeFloor() {
    camera.zoom = 1f;
    camera.position.set(map.width() / 2f, map.height() / 2f, 0f);

    updateTitle();
  }

  private void focusOnBoss() {
    RoomNode boss = layout.boss();

    if (boss != null) {
      focusOn(layout.graph().rooms().indexOf(boss));
    }
  }

  private void focusOn(int index) {
    List<RoomNode> rooms = layout.graph().rooms();

    focusRoom = Math.floorMod(index, rooms.size());

    RoomNode room = rooms.get(focusRoom);

    camera.zoom = Math.max(MIN_ZOOM, (float) RoomGeometry.CELL_WIDTH / map.width() * 1.6f);
    camera.position.set(room.anchor().centerTileX() + 0.5f, worldY(room.anchor().centerTileY()), 0f);

    updateTitle();
  }

  private float worldY(int tileY) {
    return map.height() - tileY - 0.5f;
  }

  private void exportFloor() {
    try {
      tiles.exportMap(map, tilesetIds.get(0), "export/floor-%s-%d.tmx".formatted(archetype.id(), seed));

      logger.log("Exported TMX for seed " + seed);
    } catch (TileException exception) {
      logger.log("Export failed: " + exception.getMessage());
    }
  }

  private void updateTitle() {
    RoomNode room = layout.graph().rooms().get(focusRoom);

    Gdx.graphics.setTitle(
        "%s %d - %s %d/%d %s - %d rooms, critical %d, holes %d, quests %d - focus %s %s depth %d - zoom %.2f"
            .formatted(archetype.id(), seed, replay.stage(), replay.step(), replay.totalSteps(),
                playing ? "playing" : "paused", layout.rooms(), layout.criticalLength(),
                layout.holeRooms(), layout.quests().size(), room.type(), room.variant().id(),
                room.depth(), camera.zoom)
            + " - [TAB] archetype [R] seed [SPACE] play [.] step [ENTER] finish [O] save [L] load [F] fit");
  }
}
