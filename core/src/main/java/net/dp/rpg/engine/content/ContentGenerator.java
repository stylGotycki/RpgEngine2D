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
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileRole;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.room.RoomEdge;

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
    int maxDepth = layout.graph().rooms().stream().mapToInt(RoomNode::depth).max().orElse(0);
    Map<RoomNode, RoomContent> rooms = new IdentityHashMap<>();

    for (RoomNode room : layout.graph().rooms()) {
      rooms.put(room, place(layout, room, interiors.apply(room), maxDepth));
    }

    return new FloorContent(rooms);
  }

  RoomContent place(FloorLayout layout, RoomNode room, GeneratedRoom generated, int maxDepth) {
    Random random = RandomSource.derive(layout.seed(), "content:" + room.index());
    Tiles tiles = new Tiles(generated);
    List<ContentSpot> spots = new ArrayList<>();
    double depth = maxDepth == 0 ? 0 : (double) room.depth() / maxDepth;
    QuestBundle quest = layout.questOf(room);
    boolean questGiver = quest != null && quest.entrance() == room;

    if (questGiver) {
      placeQuestGiver(tiles, entryDoors(layout, room, generated), quest.id(), random, spots);
    }

    placeItems(room, quest != null, tiles, random, spots);

    if (!questGiver) {
      placeEnemies(room.type(), depth, tiles, random, spots);
    }

    return new RoomContent(spots);
  }

  private void placeQuestGiver(Tiles tiles, List<int[]> entries, String questId, Random random,
      List<ContentSpot> spots) {
    int target = settings.npcDoorClearance() + 1;
    List<int[]> pool = tiles.floor(settings.npcDoorClearance(), random);

    pool.sort(Comparator.comparingInt(tile -> Math.abs(distance(tile, entries) - target)));

    for (int[] tile : pool) {
      if (!tiles.cutsOff(tile)) {
        spots.add(new ContentSpot(ContentKind.NPC, ContentTags.QUEST_GIVER, questId, tile[0],
            tile[1]));
        return;
      }
    }
  }

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
        boolean potion = random.nextDouble() < settings.potionChance();

        if (potion && !inQuest) {
          place(ContentKind.ITEM, ContentTags.POTION, tiles.floor(2, random), spots);
        }
      }
      default -> {
      }
    }
  }

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

    for (int i = 0; i < count; i++) {
      place(ContentKind.ITEM, ContentTags.SHOP_OFFER, pool, spots);
    }
  }

  private void placeFar(ContentKind kind, String tag, Tiles tiles, Random random,
      List<ContentSpot> spots) {
    List<int[]> pool = tiles.floor(2, random);

    pool.sort(Comparator.comparingInt((int[] tile) -> tiles.doorDistance(tile)).reversed());
    place(kind, tag, pool, spots);
  }

  private void placeEnemies(RoomType type, double depth, Tiles tiles, Random random,
      List<ContentSpot> spots) {
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

  private void place(ContentKind kind, String tag, List<int[]> pool, List<ContentSpot> spots) {
    for (int spacing = settings.spacing(); spacing >= 1; spacing--) {
      for (int[] tile : pool) {
        if (farFromAll(tile, spots, spacing)) {
          spots.add(ContentSpot.of(kind, tag, tile[0], tile[1]));
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

  private final class Tiles {

    private final TileMapData map;

    private final List<int[]> doors = new ArrayList<>();

    private final List<int[]> floor = new ArrayList<>();

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

    List<int[]> floor(int clearance, Random random) {
      List<int[]> pool = new ArrayList<>();

      for (int[] tile : floor) {
        if (doorDistance(tile) >= clearance) {
          pool.add(tile);
        }
      }

      Collections.shuffle(pool, random);

      return pool;
    }

    boolean isFloor(int[] tile) {
      return tile[0] >= 0 && tile[1] >= 0 && tile[0] < map.width() && tile[1] < map.height()
          && isPlainFloor(tile[0], tile[1]) && doorDistance(tile) > 0;
    }

    int doorDistance(int[] tile) {
      return distance(tile, doors);
    }

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
