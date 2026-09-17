package net.dp.rpg.demo;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.demo.floor.DemoFloorGenerator;
import net.dp.rpg.demo.floor.DemoRoom;
import net.dp.rpg.demo.floor.DemoRoomPainter;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileDebug;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;
import net.dp.rpg.engine.tile.render.TileRenderService;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.tiled.TmxMapExporter;
import net.dp.rpg.engine.tile.tiled.TsxTilesetSource;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetManager;

public final class FloorDemoApp extends ApplicationAdapter {

  private static final String[] TILESET_PATHS = {"tiles/terrain.tsx", "tiles/basement.tsx"};

  private static final long SEED = 20260918L;

  private static final int ROOM_COUNT = 12;

  private final List<String> tilesetIds = new ArrayList<>();

  private final List<DemoRoom> rooms = new ArrayList<>();

  private TileTypeRegistry typeRegistry;

  private TilesetManager tilesetManager;

  private TileRenderService renderService;

  private TileMapRenderer renderer;

  private TileDebug debug;

  private TileLogger logger;

  private OrthographicCamera camera;

  private Viewport viewport;

  private int activeRoom;

  private int activeTileset;

  @Override
  public void create() {
    typeRegistry = new TileTypeRegistry();
    tilesetManager = new TilesetManager(new TsxTilesetSource(), typeRegistry);

    for (String path : TILESET_PATHS) {
      tilesetIds.add(tilesetManager.load(path).id());
    }

    debug = new TileDebug(typeRegistry);
    logger = new GdxTileLogger("Floor");

    generateFloor();

    camera = new OrthographicCamera();
    renderService = new TileRenderService(tilesetManager, typeRegistry, SEED);
    renderer = new TileMapRenderer(renderService.palette(tilesetIds.getFirst()));

    logger.log(debug.describeCatalog(tilesetManager));

    showRoom(0);
  }

  private void generateFloor() {
    DemoRoomPainter painter = new DemoRoomPainter(typeRegistry);

    new DemoFloorGenerator(9, 7).generate(SEED, ROOM_COUNT)
        .forEach(draft -> rooms.add(painter.paint(draft, SEED)));

    logger.log("Generated floor: %d rooms, %d cells total".formatted(rooms.size(),
        rooms.stream().mapToInt(room -> room.shape().size()).sum()));

    for (DemoRoom room : rooms) {
      logger.log("  %s at %s: %dx%d cells, %d door(s) -> %s".formatted(room.label(),
          room.origin(), room.shape().cellsAcross(), room.shape().cellsDown(),
          room.doors().size(), room.doors().stream().map(RoomEdge::direction).toList()));
    }
  }

  @Override
  public void render() {
    handleInput();

    ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

    viewport.apply();
    camera.update();

    renderer.render(rooms.get(activeRoom).map(), camera);
  }

  @Override
  public void resize(int width, int height) {
    if (viewport != null) {
      viewport.update(width, height, true);
    }
  }

  @Override
  public void dispose() {
    if (renderer != null) {
      renderer.dispose();
    }

    if (renderService != null) {
      renderService.dispose();
    }
  }

  private void handleInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
      showRoom((activeRoom + 1) % rooms.size());
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.P)) {
      showRoom((activeRoom - 1 + rooms.size()) % rooms.size());
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.T)) {
      swapTileset();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.D)) {
      logger.log(debug.dump(rooms.get(activeRoom).map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
      exportFloor();
    }
  }

  private void showRoom(int index) {
    activeRoom = index;

    DemoRoom room = rooms.get(index);

    viewport = new FitViewport(room.map().width(), room.map().height(), camera);
    viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight(), true);

    logger.log(debug.describeMap(room.map(), room.label()));

    Gdx.graphics.setTitle("Floor demo - %s (%d/%d), tileset: %s  [N/P] room  [T] tileset  [E] export"
        .formatted(room.label(), activeRoom + 1, rooms.size(), tilesetIds.get(activeTileset)));
  }

  private void swapTileset() {
    int next = (activeTileset + 1) % tilesetIds.size();

    try {
      renderService.swap(tilesetIds.get(next), rooms.get(activeRoom).map(), renderer);
      activeTileset = next;

      showRoom(activeRoom);
    } catch (TileException exception) {
      logger.log("Swap refused: " + exception.getMessage());
    }
  }

  private void exportFloor() {
    TmxMapExporter exporter = new TmxMapExporter(typeRegistry);
    TilesetDefinition tileset = tilesetManager.require(tilesetIds.get(activeTileset));

    for (DemoRoom room : rooms) {
      exporter.export(room.map(), tileset, "export/floor1/%s.tmx".formatted(room.label()));
    }

    logger.log("Exported %d rooms as '%s'".formatted(rooms.size(), tileset.id()));
  }
}
