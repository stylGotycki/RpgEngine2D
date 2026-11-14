package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.utils.TimeUtils;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.adaptation.FloorMetrics;
import net.dp.rpg.engine.content.ContentKind;
import net.dp.rpg.engine.content.ContentSpot;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.level.events.EnemyKilledEvent;
import net.dp.rpg.game.level.events.EnterRoomEvent;
import net.dp.rpg.game.level.events.ItemPickedEvent;
import net.dp.rpg.game.level.events.NpcActionEvent;
import net.dp.rpg.game.level.events.PlayerDamagedEvent;
import net.dp.rpg.game.level.events.PotionUsedEvent;
import net.dp.rpg.game.level.events.PuzzleSolvedEvent;
import net.dp.rpg.game.level.events.QuestEvent;
import net.dp.rpg.game.scripts.NpcScript;

// Counts what the player does on one floor. Totals come from the floor's generated content, the
// rest from game events; close() stops listening, so a finished floor never counts the next one.
public class MetricsCollector implements GameEventListener {

  public static final List<Class<? extends GameEvent>> EVENTS = List.of(EnterRoomEvent.class,
      EnemyKilledEvent.class, PlayerDamagedEvent.class, PotionUsedEvent.class,
      ItemPickedEvent.class, PuzzleSolvedEvent.class, QuestEvent.class, NpcActionEvent.class);

  private final TileFloor floor;

  private final int enemiesTotal;

  private final int itemsTotal;

  private final int puzzlesTotal;

  private final long startNanos = TimeUtils.nanoTime();

  private final Set<Integer> visited = new HashSet<>();

  private final Map<GoalType, Integer> accepted = new EnumMap<>(GoalType.class);

  private int killed;

  private int picked;

  private int solved;

  private int talkCorrect;

  private int talkWrong;

  private float damage;

  private int potions;

  private int declined;

  private int completed;

  private boolean open = true;

  public MetricsCollector(TileFloor floor) {
    this.floor = floor;

    int enemies = 0;
    int items = 0;
    int puzzles = 0;

    for (TileRoom room : floor.getRooms()) {
      for (ContentSpot spot : room.content().spots()) {
        if (spot.kind() == ContentKind.ENEMY) {
          enemies++;
        } else if (spot.kind() == ContentKind.ITEM && !spot.tag().equals(ContentTags.SHOP_OFFER)) {
          // A shop lets the player take one offer; picking there is buying, not exploring.
          items++;
        }
      }
      puzzles += room.content().puzzles().size();
    }

    enemiesTotal = enemies;
    itemsTotal = items;
    puzzlesTotal = puzzles;
  }

  public void subscribe() {
    EVENTS.forEach(event -> Engine.eventBus.subscribe(this, event));
  }

  public void close() {
    open = false;
    EVENTS.forEach(event -> Engine.eventBus.unsubscribe(this, event));
  }

  @Override
  public void onEvent(GameEvent event) {
    if (!open) {
      return;
    }

    switch (event) {
      case EnterRoomEvent e when floor.owns(e.room) -> visited.add(e.room.id());
      case EnemyKilledEvent e -> killed++;
      case PlayerDamagedEvent e -> damage += e.amount;
      case PotionUsedEvent e -> potions++;
      // Quest items count for the quest, shop offers for nothing; rewards that drop later
      // (quests, puzzles) are capped by the ratio.
      case ItemPickedEvent e when !e.fromShop && e.questId == null -> picked++;
      case PuzzleSolvedEvent e -> solved++;
      case QuestEvent e -> onQuest(e);
      case NpcActionEvent e when NpcScript.TALK_CORRECT.equals(e.action) -> talkCorrect++;
      case NpcActionEvent e when NpcScript.TALK_WRONG.equals(e.action) -> talkWrong++;
      default -> {
      }
    }
  }

  private void onQuest(QuestEvent event) {
    switch (event.kind) {
      case ACCEPTED -> accepted.merge(goalType(event.template), 1, Integer::sum);
      case DECLINED -> declined++;
      case COMPLETED -> completed++;
      default -> {
      }
    }
  }

  private static GoalType goalType(String template) {
    try {
      return GoalType.valueOf(template);
    } catch (IllegalArgumentException | NullPointerException e) {
      return GoalType.COLLECT;
    }
  }

  // Safe to call every frame: the overlay shows the floor so far.
  public FloorMetrics snapshot() {
    return new FloorMetrics(floor.getRooms().size(), visited.size(), enemiesTotal, killed,
        itemsTotal, picked, puzzlesTotal, solved, talkCorrect, talkWrong, damage, potions,
        floor.getQuests().size(), accepted, declined, completed,
        (TimeUtils.nanoTime() - startNanos) / 1e9);
  }
}
