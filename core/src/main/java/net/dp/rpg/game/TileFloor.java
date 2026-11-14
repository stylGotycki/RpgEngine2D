package net.dp.rpg.game;

import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.content.ContentGenerator;
import net.dp.rpg.engine.content.ContentSettings;
import net.dp.rpg.engine.content.FloorContent;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorGenerator;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.interior.InteriorContext;
import net.dp.rpg.engine.interior.InteriorGenerator;
import net.dp.rpg.engine.interior.pass.RoomReachability;
import net.dp.rpg.engine.quest.Quest;
import net.dp.rpg.engine.quest.QuestContent;
import net.dp.rpg.engine.quest.QuestPlanner;
import net.dp.rpg.engine.quest.QuestWeights;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.game.level.events.EnterRoomEvent;
import net.dp.rpg.game.level.PlaceholderInterior;

public class TileFloor implements GameEventListener {

  private static TileSystem sharedTiles;

  private static InteriorContext sharedContext;

  private final TileLogger logger = new GdxTileLogger("Floor");

  @Getter
  private final List<TileRoom> rooms = new ArrayList<>();

  @Getter
  private final FloorLayout layout;

  @Getter
  private final TileSystem tileSystem;

  @Getter
  private final RoomReachability walkability;

  private final Scene scene;

  private int activeRoom;

  @Getter
  private final FloorContent content;

  @Getter
  private final List<Quest> quests;

  public TileFloor(FloorArchetype archetype, FloorSettings settings, long seed, Scene scene) {
    this(archetype, settings, ContentSettings.DEFAULTS, QuestWeights.DEFAULTS, seed, scene);
  }

  public TileFloor(FloorArchetype archetype, FloorSettings settings,
                   ContentSettings contentSettings, QuestWeights questWeights, long seed, Scene scene) {
    this.scene = scene;
    this.tileSystem = tiles();
    this.walkability = new RoomReachability(tileSystem.types());

    InteriorGenerator interiors = PlaceholderInterior.generator(sharedContext, tileSystem);

    layout = new FloorGenerator().generate(archetype, settings, seed);

    Map<RoomNode, GeneratedRoom> generated = new IdentityHashMap<>();

    for (RoomNode node : layout.graph().rooms()) {
      generated.put(node, interiors.generate(layout.blueprintOf(node)));
    }

    List<Quest> planned = new QuestPlanner(questWeights).plan(layout);
    content = new ContentGenerator(tileSystem.types(), contentSettings)
        .generate(layout, generated::get, planned);
    quests = QuestContent.reconcile(planned, layout, content);

    for (RoomNode node : layout.graph().rooms()) {
      rooms.add(new TileRoom(node.index(), node, generated.get(node), content.of(node)));
    }

    activeRoom = layout.graph().start().index();
    logSummary(seed);
    showActiveRoom();
  }

  private static synchronized TileSystem tiles() {
    if (sharedTiles == null) {
      sharedTiles = new TileSystem(1L);
      sharedTiles.loadTileset(PlaceholderInterior.TILESET);
      sharedContext = PlaceholderInterior.load(sharedTiles);
    }

    return sharedTiles;
  }

  public TileRoom getActiveRoom() {
    return rooms.get(activeRoom);
  }

  public TileRoom getStartRoom() {
    return rooms.get(layout.graph().start().index());
  }

  public TileRoom roomBehind(TileRoom room, RoomEdge localEdge) {
    RoomCell outside = room.toFloorCell(localEdge.cell()).neighbour(localEdge.direction());
    RoomNode node = layout.graph().roomAt(outside);

    return node == null ? null : rooms.get(node.index());
  }

  public boolean owns(TileRoom room) {
    return room != null && room.id() < rooms.size() && rooms.get(room.id()) == room;
  }

  public void setRoomActive(TileRoom room) {
    if (!owns(room)) {
      return;
    }

    activeRoom = room.id();
    showActiveRoom();
  }

  public int[] walkableTileNear(TileRoom room, int x, int y) {
    int limit = Math.max(room.map().width(), room.map().height());

    for (int radius = 0; radius < limit; radius++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
          boolean onRing = Math.abs(dx) == radius || Math.abs(dy) == radius;

          if (onRing && walkability.isWalkable(room.map(), x + dx, y + dy)
              && !isDoorTile(room, x + dx, y + dy)) {
            return new int[] {x + dx, y + dy};
          }
        }
      }
    }

    return new int[] {x, y};
  }

  private static boolean isDoorTile(TileRoom room, int x, int y) {
    return room.doors().stream().anyMatch(edge -> edge.doorTileX() == x && edge.doorTileY() == y);
  }

  private void showActiveRoom() {
    TileRoom room = getActiveRoom();

    scene.setTileMap(room.map(), tileSystem, room.origin().x(), room.origin().y());
  }

  private void logSummary(long seed) {
    long fallbacks = rooms.stream().filter(room -> room.generated().isFallback()).count();

    logger.log("Floor %s seed %d: %d rooms, critical path %d, %d fallback interiors%n%s%n%s"
        .formatted(layout.archetypeId(), seed, rooms.size(), layout.criticalLength(), fallbacks,
            layout.summary(), "content " + content.totals() + ", quests " + quests.stream()
                .map(quest -> quest.id() + "=" + quest.template()).toList()));
  }

  @Override
  public void onEvent(GameEvent event) {
    // The event bus outlives scenes, so a floor of an earlier game may still be listening.
    if (event instanceof EnterRoomEvent roomEvent && owns(roomEvent.room)) {
      setRoomActive(roomEvent.room);
    }
  }
}
