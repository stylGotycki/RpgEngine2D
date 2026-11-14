package net.dp.rpg.engine.content;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.Function;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.QuestBundle;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.GeneratedRoom;
import net.dp.rpg.engine.interior.pass.RoomReachability;
import net.dp.rpg.engine.puzzle.PuzzleKind;
import net.dp.rpg.engine.puzzle.PuzzleSpec;
import net.dp.rpg.engine.quest.Quest;
import net.dp.rpg.engine.quest.QuestContent;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileRole;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.room.RoomEdge;

/**
 * Places what stands in the rooms of a generated floor: enemies, items and NPCs (E1). Runs after
 * the interiors, because spots must lie on real floor tiles of the room's map.
 *
 * <p>Rules, all checked by {@code ContentPlacementTest}:
 * <ul>
 *   <li>every spot is a plain floor tile (no door, wall, hazard, pillar or detail on it);
 *   <li>enemies keep {@link ContentSettings#enemyDoorClearance()} tiles from every door;
 *   <li>an NPC never cuts any floor of its room off from the doors;
 *   <li>START, SHOP, PUZZLE and EMPTY rooms and a quest giver's room hold no enemies;
 *   <li>the enemy count grows with depth (difficulty control) and scales with
 *       {@link ContentSettings#difficulty()};
 *   <li>each room draws from its own random stream, so changing one room's content (or the
 *       difficulty) moves nothing in the others, and one seed always gives the same content.
 * </ul>
 */
public final class ContentGenerator {

  private final TileTypeRegistry types;

  private final RoomReachability reachability;

  private final ContentSettings settings;

  public ContentGenerator(TileTypeRegistry types, ContentSettings settings) {
    this.types = types;
    this.reachability = new RoomReachability(types);
    this.settings = settings;
  }

  public FloorContent generate(FloorLayout layout, Function<RoomNode, GeneratedRoom> interiors) {
    return generate(layout, interiors, List.of());
  }

  public FloorContent generate(FloorLayout layout, Function<RoomNode, GeneratedRoom> interiors,
      List<Quest> quests) {
    int maxDepth = layout.graph().rooms().stream().mapToInt(RoomNode::depth).max().orElse(0);
    Map<RoomNode, RoomContent> rooms = new IdentityHashMap<>();

    for (RoomNode room : layout.graph().rooms()) {
      rooms.put(room, place(layout, room, interiors.apply(room), maxDepth, quests));
    }

    return new FloorContent(rooms);
  }

  RoomContent place(FloorLayout layout, RoomNode room, GeneratedRoom generated, int maxDepth,
      List<Quest> quests) {
    Random random = RandomSource.derive(layout.seed(), "content:" + room.index());
    Tiles tiles = new Tiles(generated);
    List<ContentSpot> spots = new ArrayList<>();
    double depth = maxDepth == 0 ? 0 : (double) room.depth() / maxDepth;
    QuestBundle quest = layout.questOf(room);
    boolean questGiver = quest != null && quest.entrance() == room;

    List<PuzzleSpec> puzzles = new ArrayList<>();
    Random puzzleRandom = RandomSource.derive(layout.seed(), "puzzle:" + room.index());

    // A quest's puzzle in a puzzle room is that room's puzzle; one board per room.
    List<QuestContent.PuzzleRequest> questPuzzles = QuestContent.puzzles(quests, room.index());
    PuzzleKind ownKind = PuzzleKind.values()[puzzleRandom.nextInt(PuzzleKind.values().length)];

    for (QuestContent.PuzzleRequest request : questPuzzles) {
      placePuzzle(tiles, request.kind(), request.questId(), puzzleRandom, puzzles);
    }
    if (room.type() == RoomType.PUZZLE && questPuzzles.isEmpty()) {
      placePuzzle(tiles, ownKind, null, puzzleRandom, puzzles);
    }

    if (questGiver) {
      placeQuestGiver(tiles, entryDoors(layout, room, generated), quest.id(), random, spots);
    }

    for (QuestContent.TalkerRequest talker : QuestContent.talkers(quests, room.index())) {
      placeNpc(tiles, tiles.floor(settings.npcDoorClearance(), random),
          ContentTags.talker(talker.dialogueId()), talker.questId(), spots);
    }

    placeItems(room, quest != null, tiles, random, spots);

    for (QuestContent.ItemRequest item : QuestContent.items(quests, room.index())) {
      place(ContentKind.ITEM, ContentTags.questItem(item.itemId()), item.questId(),
          tiles.floor(2, random), spots);
    }

    if (!questGiver) {
      placeEnemies(room.type(), depth, tiles, random, spots,
          QuestContent.enemies(quests, room.index()));
    }

    return new RoomContent(spots, puzzles);
  }

  // Plain floor near the middle of the room, two tiles from every door; its tiles and a margin
  // of one are kept free of anything else.
  // The wanted kind first, then the others from the smallest, for rooms too tight for it.
  private void placePuzzle(Tiles tiles, PuzzleKind kind, String questId, Random random,
      List<PuzzleSpec> puzzles) {
    long seed = random.nextLong();
    List<PuzzleKind> kinds = new ArrayList<>(List.of(kind));

    java.util.Arrays.stream(PuzzleKind.values())
        .filter(other -> other != kind)
        .sorted(Comparator.comparingInt(other -> other.width() * other.height()))
        .forEach(kinds::add);

    for (PuzzleKind candidate : kinds) {
      int[] corner = tiles.area(candidate.width(), candidate.height());

      if (corner != null) {
        puzzles.add(new PuzzleSpec(candidate, corner[0], corner[1], seed, questId));
        tiles.block(corner[0] - 1, corner[1] - 1, candidate.width() + 2,
            candidate.height() + 2);
        return;
      }
    }
  }

  private void placeQuestGiver(Tiles tiles, List<int[]> entries, String questId, Random random,
      List<ContentSpot> spots) {
    int target = settings.npcDoorClearance() + 1;
    List<int[]> pool = tiles.floor(settings.npcDoorClearance(), random);

    // Close to the door the player comes in through, so the giver is the first thing they meet,
    // but not in the way.
    pool.sort(Comparator.comparingInt(tile -> Math.abs(distance(tile, entries) - target)));
    placeNpc(tiles, pool, ContentTags.QUEST_GIVER, questId, spots);
  }

  private void placeNpc(Tiles tiles, List<int[]> pool, String tag, String questId,
      List<ContentSpot> spots) {
    for (int[] tile : pool) {
      if (!tiles.cutsOff(tile) && farFromAll(tile, spots, 1)) {
        spots.add(new ContentSpot(ContentKind.NPC, tag, questId, tile[0], tile[1]));
        return;
      }
    }
  }

  /**
   * Door tiles towards shallower rooms: the way the player arrives. All doors when there is none
   * (only START), so the result is never empty for a room with doors.
   */
  private static List<int[]> entryDoors(FloorLayout layout, RoomNode room,
      GeneratedRoom generated) {
    List<int[]> entries = new ArrayList<>();
    List<int[]> all = new ArrayList<>();

    for (RoomEdge door : generated.blueprint().doors().keySet()) {
      int[] tile = {door.doorTileX(), door.doorTileY()};
      RoomNode behind = layout.graph().roomAt(
          room.toFloorCell(door.cell()).neighbour(door.direction()));

      all.add(tile);
      if (behind != null && behind.depth() < room.depth()) {
        entries.add(tile);
      }
    }

    return entries.isEmpty() ? all : entries;
  }

  private static int distance(int[] tile, List<int[]> targets) {
    int best = Integer.MAX_VALUE;

    for (int[] target : targets) {
      best = Math.min(best, Math.max(Math.abs(target[0] - tile[0]), Math.abs(target[1] - tile[1])));
    }

    return best;
  }

  private void placeItems(RoomNode room, boolean inQuest, Tiles tiles, Random random,
      List<ContentSpot> spots) {
    switch (room.type()) {
      case SHOP -> placeShop(tiles, random, spots);
      case VAULT -> placeFar(ContentKind.ITEM, ContentTags.EQUIPMENT, tiles, random, spots);
      case COLLECTIBLE -> placeFar(ContentKind.ITEM, ContentTags.LOOT, tiles, random, spots);
      case NORMAL, EMPTY -> {
        // Drawn even in quest rooms, so a quest does not shift the other rooms' streams.
        boolean potion = random.nextDouble() < settings.potionChance();

        if (potion && !inQuest) {
          // The extra draw only exists with a healing share, so default floors keep their streams.
          boolean healing = settings.healingShare() > 0
              && random.nextDouble() < settings.healingShare();
          place(ContentKind.ITEM, healing ? ContentTags.HEALING_POTION : ContentTags.POTION,
              tiles.floor(2, random), spots);
        }
      }
      default -> {
      }
    }
  }

  /** The offers in a row through the middle of the room, two tiles apart. */
  private void placeShop(Tiles tiles, Random random, List<ContentSpot> spots) {
    int count = settings.shopOffers();
    List<int[]> pool = tiles.floor(2, random);
    double centerX = pool.stream().mapToInt(tile -> tile[0]).average().orElse(0);
    double centerY = pool.stream().mapToInt(tile -> tile[1]).average().orElse(0);

    pool.sort(Comparator.comparingDouble(
        tile -> Math.hypot(tile[0] - centerX, tile[1] - centerY)));

    for (int[] middle : pool) {
      List<int[]> row = new ArrayList<>();

      for (int i = 0; i < count; i++) {
        int[] tile = {middle[0] + 2 * (i - count / 2), middle[1]};

        if (tiles.isFloor(tile) && tiles.doorDistance(tile) >= 2) {
          row.add(tile);
        }
      }

      if (row.size() == count) {
        row.forEach(tile -> spots.add(
            ContentSpot.of(ContentKind.ITEM, ContentTags.SHOP_OFFER, tile[0], tile[1])));
        return;
      }
    }

    // A room too narrow for a row: anywhere, still apart.
    for (int i = 0; i < count; i++) {
      place(ContentKind.ITEM, ContentTags.SHOP_OFFER, pool, spots);
    }
  }

  /** Farthest from the doors: a reward at the back of the room. */
  private void placeFar(ContentKind kind, String tag, Tiles tiles, Random random,
      List<ContentSpot> spots) {
    List<int[]> pool = tiles.floor(2, random);

    pool.sort(Comparator.comparingInt((int[] tile) -> tiles.doorDistance(tile)).reversed());
    place(kind, tag, pool, spots);
  }

  private void placeEnemies(RoomType type, double depth, Tiles tiles, Random random,
      List<ContentSpot> spots, Map<String, Integer> questNeeds) {
    ContentSettings.Range range = settings.enemiesOf(type);
    double base = range.min() + random.nextInt(range.max() - range.min() + 1);

    if (type == RoomType.NORMAL) {
      base += settings.depthBonus() * depth;
    }

    int count = (int) Math.round(base * settings.difficulty());
    List<int[]> pool = tiles.floor(settings.enemyDoorClearance(), random);

    if (type == RoomType.BOSS || type == RoomType.MINIBOSS) {
      List<int[]> far = new ArrayList<>(pool);

      far.sort(Comparator.comparingInt((int[] tile) -> tiles.doorDistance(tile)).reversed());
      place(ContentKind.ENEMY, type == RoomType.BOSS ? ContentTags.BOSS : ContentTags.MINIBOSS,
          far, spots);
    }

    for (int i = 0; i < count; i++) {
      place(ContentKind.ENEMY, enemyType(depth, random), pool, spots);
    }

    // Quests may need more of one kind than the room drew.
    questNeeds.forEach((tag, needed) -> {
      long present = spots.stream()
          .filter(spot -> spot.kind() == ContentKind.ENEMY && spot.tag().equals(tag)).count();

      for (long i = present; i < needed; i++) {
        place(ContentKind.ENEMY, tag, pool, spots);
      }
    });
  }

  private String enemyType(double depth, Random random) {
    double total = settings.enemyTypes().stream().mapToDouble(type -> type.at(depth)).sum();
    double roll = random.nextDouble() * total;

    for (ContentSettings.EnemyWeight type : settings.enemyTypes()) {
      roll -= type.at(depth);

      if (roll < 0) {
        return type.tag();
      }
    }

    return settings.enemyTypes().get(settings.enemyTypes().size() - 1).tag();
  }

  /**
   * Takes the first tile of the pool far enough from every spot so far, relaxing the spacing
   * down to "any free tile" in rooms too small for it. Places nothing when the pool is used up.
   */
  private void place(ContentKind kind, String tag, List<int[]> pool, List<ContentSpot> spots) {
    place(kind, tag, null, pool, spots);
  }

  private void place(ContentKind kind, String tag, String questId, List<int[]> pool,
      List<ContentSpot> spots) {
    for (int spacing = settings.spacing(); spacing >= 1; spacing--) {
      for (int[] tile : pool) {
        if (farFromAll(tile, spots, spacing)) {
          spots.add(new ContentSpot(kind, tag, questId, tile[0], tile[1]));
          return;
        }
      }
    }
  }

  private static boolean farFromAll(int[] tile, List<ContentSpot> spots, int spacing) {
    for (ContentSpot spot : spots) {
      if (Math.max(Math.abs(spot.x() - tile[0]), Math.abs(spot.y() - tile[1])) < spacing) {
        return false;
      }
    }

    return true;
  }

  /** The tiles of one room's map, as content placement sees them. */
  private final class Tiles {

    private final TileMapData map;

    private final List<int[]> doors = new ArrayList<>();

    private final List<int[]> floor = new ArrayList<>();

    private final java.util.Set<Integer> blocked = new java.util.HashSet<>();

    Tiles(GeneratedRoom generated) {
      this.map = generated.map();

      for (RoomEdge door : generated.blueprint().doors().keySet()) {
        doors.add(new int[] {door.doorTileX(), door.doorTileY()});
      }

      for (int y = 0; y < map.height(); y++) {
        for (int x = 0; x < map.width(); x++) {
          int[] tile = {x, y};

          if (isPlainFloor(x, y) && doorDistance(tile) > 0) {
            floor.add(tile);
          }
        }
      }
    }

    /** Plain floor at least {@code clearance} tiles from every door, in a random order. */
    List<int[]> floor(int clearance, Random random) {
      List<int[]> pool = new ArrayList<>();

      for (int[] tile : floor) {
        if (doorDistance(tile) >= clearance && !blocked.contains(key(tile[0], tile[1]))) {
          pool.add(tile);
        }
      }

      Collections.shuffle(pool, random);

      return pool;
    }

    boolean isFloor(int[] tile) {
      return tile[0] >= 0 && tile[1] >= 0 && tile[0] < map.width() && tile[1] < map.height()
          && isPlainFloor(tile[0], tile[1]) && doorDistance(tile) > 0
          && !blocked.contains(key(tile[0], tile[1]));
    }

    private int key(int x, int y) {
      return y * map.width() + x;
    }

    void block(int x, int y, int width, int height) {
      for (int dy = 0; dy < height; dy++) {
        for (int dx = 0; dx < width; dx++) {
          blocked.add(key(x + dx, y + dy));
        }
      }
    }

    // The bottom-left corner of the free width x height rectangle closest to the middle of the
    // room's floor, or null when none fits.
    int[] area(int width, int height) {
      double centerX = floor.stream().mapToInt(tile -> tile[0]).average().orElse(0);
      double centerY = floor.stream().mapToInt(tile -> tile[1]).average().orElse(0);
      int[] best = null;
      double bestDistance = Double.MAX_VALUE;

      for (int y = 0; y + height <= map.height(); y++) {
        for (int x = 0; x + width <= map.width(); x++) {
          double distance = Math.hypot(x + width / 2.0 - centerX, y + height / 2.0 - centerY);

          if (distance < bestDistance && fits(x, y, width, height)) {
            best = new int[] {x, y};
            bestDistance = distance;
          }
        }
      }

      return best;
    }

    private boolean fits(int x, int y, int width, int height) {
      for (int dy = 0; dy < height; dy++) {
        for (int dx = 0; dx < width; dx++) {
          int[] tile = {x + dx, y + dy};

          if (!isFloor(tile) || doorDistance(tile) < 2) {
            return false;
          }
        }
      }

      return true;
    }

    int doorDistance(int[] tile) {
      return distance(tile, doors);
    }

    /** Whether blocking the tile would leave some walkable tile unreachable from the doors. */
    boolean cutsOff(int[] blocked) {
      return reachable(null) - 1 != reachable(blocked);
    }

    private int reachable(int[] blocked) {
      if (doors.isEmpty()) {
        return 0;
      }

      boolean[] seen = new boolean[map.width() * map.height()];
      Deque<int[]> pending = new ArrayDeque<>();
      int count = 0;

      for (int[] door : doors) {
        if (!seen[door[1] * map.width() + door[0]]) {
          seen[door[1] * map.width() + door[0]] = true;
          pending.add(door);
        }
      }

      while (!pending.isEmpty()) {
        int[] tile = pending.poll();
        count++;

        for (int[] step : new int[][] {{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
          int x = tile[0] + step[0];
          int y = tile[1] + step[1];

          if (x < 0 || y < 0 || x >= map.width() || y >= map.height()
              || seen[y * map.width() + x]
              || (blocked != null && blocked[0] == x && blocked[1] == y)
              || !reachability.isWalkable(map, x, y)) {
            continue;
          }

          seen[y * map.width() + x] = true;
          pending.add(new int[] {x, y});
        }
      }

      return count;
    }

    private boolean isPlainFloor(int x, int y) {
      int ground = map.requireLayer(TileLayerKind.GROUND).grid().getOrEmpty(x, y);

      if (ground == TileGrid.EMPTY || types.require(ground).role() != TileRole.FLOOR) {
        return false;
      }

      int detail = map.findLayer(TileLayerKind.DETAILS)
          .map(layer -> layer.grid().getOrEmpty(x, y))
          .orElse(TileGrid.EMPTY);

      return detail == TileGrid.EMPTY && reachability.isWalkable(map, x, y);
    }
  }
}
