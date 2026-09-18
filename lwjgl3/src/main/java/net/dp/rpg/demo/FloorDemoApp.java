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
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.engine.tile.tiled.TmxMapExporter;
import net.dp.rpg.engine.tile.tiled.TsxTilesetSource;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetManager;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.tiled.TmxMapSource;
import net.dp.rpg.engine.tile.room.RoomShape;

import java.util.Set;

public final class FloorDemoApp extends ApplicationAdapter {

  private static final String MAP_PATH = "maps/room-19x13.tmx";

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

  private RoomCell focusCell;

  private int activeRoom;

  private int activeTileset;

  private boolean mapLoaded;

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

    viewport = new FitViewport(RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT, camera);

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
      viewport.update(width, height);
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

    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
      exportFloor();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
      loadRoomFromFile();
    }

    handleFocusInput();
    handleDebugInput();
  }

  private void handleFocusInput() {
    DemoRoom room = rooms.get(activeRoom);

    for (Direction direction : Direction.values()) {
      if (!Gdx.input.isKeyJustPressed(keyOf(direction))) {
        continue;
      }

      RoomCell target = focusCell.neighbour(direction);

      if (room.shape().contains(target)) {
        focusCell = target;

        centerOnFocus(room);
        updateTitle();
      }
    }
  }

  private void handleDebugInput() {
    DemoRoom room = rooms.get(activeRoom);

    if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
      logger.log(debug.dump(room.map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
      logger.log(debug.dumpWalkable(room.map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F3)) {
      logger.log(debug.describeHistogram(room.map()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F4)) {
      logger.log(debug.describeCatalog(tilesetManager));
    }
  }

  private void showRoom(int index) {
    activeRoom = index;

    DemoRoom room = rooms.get(index);

    focusCell = room.shape().topLeftCell();

    centerOnFocus(room);
    updateTitle();

    logger.log(debug.describeMap(room.map(), room.label()));
  }

  private void centerOnFocus(DemoRoom room) {
    float worldX = focusCell.centerTileX() + 0.5f;
    float worldY = room.map().height() - focusCell.centerTileY() - 0.5f;

    camera.position.set(worldX, worldY, 0f);
  }

  private void swapTileset() {
    int next = (activeTileset + 1) % tilesetIds.size();

    try {
      renderService.swap(tilesetIds.get(next), rooms.get(activeRoom).map(), renderer);
      activeTileset = next;

      updateTitle();
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

  private void updateTitle() {
    DemoRoom room = rooms.get(activeRoom);

    Gdx.graphics.setTitle("Floor demo - %s (%d/%d) cell %d,%d of %dx%d - tileset %s - [N/P] room [WASD] cell [T] tileset [E] export [L] load tmx [F1-F4] debug"
        .formatted(room.label(), activeRoom + 1, rooms.size(), focusCell.x(), focusCell.y(),
            room.shape().cellsAcross(), room.shape().cellsDown(), tilesetIds.get(activeTileset)));
  }

  private void loadRoomFromFile() {
    if (mapLoaded) {
      logger.log("Map already loaded as room %d".formatted(rooms.size() - 1));

      return;
    }

    TileMapData map = new TmxMapSource(tilesetManager, typeRegistry).load(MAP_PATH);

    if (map.width() != RoomGeometry.CELL_WIDTH || map.height() != RoomGeometry.CELL_HEIGHT) {
      logger.log("Warning: %s is %dx%d, expected one cell of %dx%d".formatted(MAP_PATH,
          map.width(), map.height(), RoomGeometry.CELL_WIDTH, RoomGeometry.CELL_HEIGHT));
    }

    rooms.add(new DemoRoom(rooms.size(), RoomShape.single(), RoomCell.ORIGIN, Set.of(), map));
    mapLoaded = true;

    logger.log("Loaded %s as room %d".formatted(MAP_PATH, rooms.size() - 1));

    showRoom(rooms.size() - 1);
  }

  private static int keyOf(Direction direction) {
    return switch (direction) {
      case NORTH -> Input.Keys.W;
      case SOUTH -> Input.Keys.S;
      case WEST -> Input.Keys.A;
      case EAST -> Input.Keys.D;
    };
  }
}
