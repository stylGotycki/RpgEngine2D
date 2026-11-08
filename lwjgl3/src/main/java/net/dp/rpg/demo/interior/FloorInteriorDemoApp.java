package net.dp.rpg.demo.interior;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.Viewport;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;
import net.dp.rpg.engine.floor.FloorGenerator;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.interior.assembly.FloorInterior;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;

public final class FloorInteriorDemoApp extends ApplicationAdapter {

  private enum LayerView {
    BOTH,
    GROUND,
    DETAILS
  }

  private static final long FIRST_SEED = 20261005L;

  private static final float MIN_ZOOM = 0.05f;

  private static final float MAX_ZOOM = 2f;

  private static final float ZOOM_STEP = 1.15f;

  private static final float PAN_TILES_PER_SECOND = 40f;

  private final FloorGenerator floorGenerator = new FloorGenerator();

  private final Map<Integer, Integer> rerolls = new HashMap<>();

  private TileSystem tiles;

  private InteriorContext context;

  private InteriorGenerator generator;

  private TileMapRenderer renderer;

  private ZoneOverlay zoneOverlay;

  private FloorOverlays overlays;

  private TileLogger logger;

  private OrthographicCamera camera;

  private Viewport viewport;

  private int archetypeIndex;

  private long seed = FIRST_SEED;

  private FloorInterior floor;

  private TileMapData view;

  private int selected;

  private LayerView layerView = LayerView.BOTH;

  private boolean showZones;

  private boolean showRoute;

  private boolean showMarkers;

  @Override
  public void create() {
    logger = new GdxTileLogger("FloorInterior");

    tiles = new TileSystem(FIRST_SEED);
    tiles.loadTilesets(PlaceholderMotif.TILESET_PATH);

    context = PlaceholderMotif.load(tiles);
    generator = PlaceholderMotif.generator(context, tiles);

    camera = new OrthographicCamera();
    renderer = tiles.createRenderer();
    zoneOverlay = new ZoneOverlay();
    overlays = new FloorOverlays();

    logger.log("Loaded corpus '%s': %d samples, %d warnings".formatted(PlaceholderMotif.MOTIF,
        context.corpus().samples().size(), context.corpus().warnings().size()));
    context.corpus().warnings().forEach(warning -> logger.log("  " + warning));

    newFloor();
  }

  private void newFloor() {
    FloorArchetype archetype = FloorArchetypes.ALL.get(archetypeIndex);
    long started = System.nanoTime();
    FloorLayout layout = floorGenerator.generate(archetype, seed);

    floor = FloorInterior.assemble(layout, generator);
    rerolls.clear();
    selected = layout.graph().start().index();

    double millis = (System.nanoTime() - started) / 1e6;

    logger.log("floor %s seed %d: %s".formatted(archetype.id(), seed, layout.summary()));
    logger.log("assembled %d rooms in %.0f ms: %d retried, %d fallback".formatted(
        floor.rooms().size(), millis, floor.retriedCount(), floor.fallbackCount()));

    for (GeneratedRoom room : floor.rooms()) {
      if (room.isFallback()) {
        logger.log("  FALLBACK %s at %s: %s".formatted(room.blueprint().type(),
            room.blueprint().anchor(), room.note()));
      }
    }

    refreshView(true);
  }

  /** Rebuilds what is drawn; {@code refit} also frames the whole floor, which a reroll must not. */
  private void refreshView(boolean refit) {
    TileMapData full = floor.map();

    view = switch (layerView) {
      case BOTH -> full;
      case GROUND -> onlyLayer(full, TileLayerKind.GROUND);
      case DETAILS -> onlyLayer(full, TileLayerKind.DETAILS);
    };

    if (refit) {
      viewport = new FitViewport(full.width(), full.height(), camera);
      viewport.update(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());
      camera.position.set(full.width() / 2f, full.height() / 2f, 0f);
      camera.zoom = 1f;
    }

    updateTitle();
  }

  private static TileMapData onlyLayer(TileMapData map, TileLayerKind kind) {
    return new TileMapData(List.of(map.requireLayer(kind)), map.objects(), map.properties());
  }

  @Override
  public void render() {
    handleInput();

    ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

    if (view == null) {
      return;
    }

    viewport.apply();
    camera.update();

    renderer.render(view, camera);
    renderOverlays();
  }

  private void renderOverlays() {
    int mapHeight = floor.map().height();
    RoomNode node = selectedNode();
    GeneratedRoom room = floor.room(selected);

    if (showZones) {
      zoneOverlay.render(room.canvas().zones(), camera, floor.tileOffsetX(node),
          floor.tileOffsetY(node), mapHeight);
    }

    if (showRoute) {
      overlays.route(room.canvas(), floor.tileOffsetX(node), floor.tileOffsetY(node), mapHeight,
          camera);
    }

    if (showMarkers) {
      overlays.markers(floor.map().objects(), mapHeight, camera);
    }

    overlays.selection(node, floor.minCellX(), floor.minCellY(), mapHeight, camera);
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

    if (zoneOverlay != null) {
      zoneOverlay.dispose();
    }

    if (overlays != null) {
      overlays.dispose();
    }

    if (tiles != null) {
      tiles.dispose();
    }
  }

  private RoomNode selectedNode() {
    return floor.layout().graph().rooms().get(selected);
  }

  private void handleInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.N)) {
      seed++;
      newFloor();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.C)) {
      archetypeIndex = (archetypeIndex + 1) % FloorArchetypes.ALL.size();
      newFloor();
    }

    if (Gdx.input.isButtonJustPressed(Input.Buttons.LEFT)) {
      selectRoomUnderCursor();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      rerollSelected();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.L)) {
      layerView = LayerView.values()[(layerView.ordinal() + 1) % LayerView.values().length];
      refreshView(false);
    }

    handleToggles();
    handleExportsAndDebug();
    handleZoom();
    handlePan();
  }

  private void handleToggles() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.Z)) {
      showZones = !showZones;
      updateTitle();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.V)) {
      showRoute = !showRoute;
      updateTitle();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.M)) {
      showMarkers = !showMarkers;
      updateTitle();
    }
  }

  private void handleExportsAndDebug() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.E)) {
      GeneratedRoom room = floor.room(selected);

      export(room.map(), "export/interior/room-%d-%s-%d.tmx".formatted(selected,
          room.blueprint().type(), room.blueprint().seed()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.X)) {
      export(floor.map(), "export/interior/floor-%s-%d.tmx".formatted(
          floor.layout().archetypeId(), floor.layout().seed()));
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F1)) {
      logger.log(floor.layout().summary());
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.F2)) {
      logger.log(describe(selectedNode(), floor.room(selected)));
    }
  }

  private void selectRoomUnderCursor() {
    Vector2 world = viewport.unproject(new Vector2(Gdx.input.getX(), Gdx.input.getY()));
    int tileX = (int) Math.floor(world.x);
    int tileY = floor.map().height() - 1 - (int) Math.floor(world.y);
    RoomNode hit = floor.roomAtTile(tileX, tileY);

    if (hit != null) {
      selected = hit.index();
      updateTitle();
    }
  }

  /**
   * Regenerates only the selected room, from the floor's own blueprint for it with a seed derived
   * from how many times it has been rerolled, so the sequence of rerolls is reproducible. The
   * layout and every other room are untouched; the new interior is stitched into the same map.
   */
  private void rerollSelected() {
    RoomNode node = selectedNode();
    int count = rerolls.merge(node.index(), 1, Integer::sum);
    RoomBlueprint original = floor.layout().blueprintOf(node);
    RoomBlueprint blueprint = original.withSeed(
        RandomSource.deriveSeed(original.seed(), "interior.reroll#" + count));
    GeneratedRoom room = generator.generate(blueprint);

    floor = floor.withRoom(node.index(), room);

    logger.log(room.isFallback()
        ? "reroll #%d of room %d: FALLBACK (%s)".formatted(count, node.index(), room.note())
        : "reroll #%d of room %d: %s".formatted(count, node.index(),
            describe(node, room)));

    refreshView(false);
  }

  private void export(TileMapData map, String path) {
    try {
      tiles.exportMap(map, PlaceholderMotif.TILESET_ID, path);
      logger.log("Exported TMX to " + path);
    } catch (TileException exception) {
      logger.log("Export failed: " + exception.getMessage());
    }
  }

  private static String describe(RoomNode node, GeneratedRoom room) {
    return "room %d %s %s doors=%d seed=%d %s%s".formatted(node.index(), room.blueprint().type(),
        room.blueprint().variant().id(), room.blueprint().doors().size(),
        room.blueprint().seed(), room.status(),
        room.note().isEmpty() ? "" : " (" + room.note() + ")");
  }

  private void handleZoom() {
    boolean zoomIn = Gdx.input.isKeyJustPressed(Input.Keys.EQUALS)
        || Gdx.input.isKeyJustPressed(Input.Keys.PLUS);

    if (zoomIn) {
      camera.zoom = Math.max(MIN_ZOOM, camera.zoom / ZOOM_STEP);
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.MINUS)) {
      camera.zoom = Math.min(MAX_ZOOM, camera.zoom * ZOOM_STEP);
    }
  }

  private void handlePan() {
    float step = PAN_TILES_PER_SECOND * camera.zoom * Gdx.graphics.getDeltaTime();

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

  private void updateTitle() {
    GeneratedRoom room = floor.room(selected);

    Gdx.graphics.setTitle(
        "Floor %s seed %d - %d rooms (%d retried, %d fallback) - selected #%d %s %s %s".formatted(
            floor.layout().archetypeId(), seed, floor.rooms().size(), floor.retriedCount(),
            floor.fallbackCount(), selected, room.blueprint().type(),
            room.blueprint().variant().id(), room.status())
            + " - layers %s zones %s route %s markers %s".formatted(layerView,
            showZones ? "ON" : "off", showRoute ? "ON" : "off", showMarkers ? "ON" : "off")
            + " - [N] floor [C] archetype [click] room [R] reroll [L] layers"
            + " [Z/V/M] overlays [E/X] export");
  }
}
