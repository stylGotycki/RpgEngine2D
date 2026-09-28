package net.dp.rpg.engine.floor.phase;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import net.dp.rpg.engine.floor.FloorContext;
import net.dp.rpg.engine.floor.QuestBundle;
import net.dp.rpg.engine.floor.QuestSettings;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;

public final class QuestPhase {

  private static final int SEED_ATTEMPTS = 12;

  public List<QuestBundle> assign(FloorGraph graph, FloorContext context) {
    QuestSettings settings = context.plan().questSettings();
    Random random = context.stream("quest");

    Set<RoomNode> free = eligible(graph, context);
    int budget = (int) Math.floor(free.size() * settings.roomRatio());

    List<QuestBundle> bundles = new ArrayList<>();

    while (budget > 0 && !free.isEmpty()) {
      int size = Math.min(settings.drawSize(random), budget);
      List<RoomNode> rooms = null;

      while (size > 0 && rooms == null) {
        rooms = findConnected(free, size, random);

        if (rooms == null) {
          size--;
        }
      }

      if (rooms == null) {
        break;
      }

      QuestBundle bundle = new QuestBundle("q" + (bundles.size() + 1), rooms);

      rooms.forEach(room -> {
        room.setQuestId(bundle.id());
        free.remove(room);
      });

      bundles.add(bundle);
      budget -= rooms.size();
    }

    return List.copyOf(bundles);
  }

  /**
   * Grows a connected group of the wanted size, starting from a random free room and spreading through doors to
   * other free rooms.
   */
  private List<RoomNode> findConnected(Set<RoomNode> free, int size, Random random) {
    List<RoomNode> seeds = new ArrayList<>(free);

    Collections.shuffle(seeds, random);

    for (int attempt = 0; attempt < Math.min(SEED_ATTEMPTS, seeds.size()); attempt++) {
      List<RoomNode> group = grow(seeds.get(attempt), free, size);

      if (group.size() == size) {
        return group;
      }
    }

    return null;
  }

  private List<RoomNode> grow(RoomNode seed, Set<RoomNode> free, int size) {
    List<RoomNode> group = new ArrayList<>();
    Set<RoomNode> taken = new LinkedHashSet<>();
    Deque<RoomNode> pending = new ArrayDeque<>();

    taken.add(seed);
    group.add(seed);
    pending.add(seed);

    while (!pending.isEmpty() && group.size() < size) {
      RoomNode current = pending.removeFirst();

      for (RoomLink link : current.links()) {
        RoomNode next = link.other(current);

        if (group.size() < size && free.contains(next) && taken.add(next)) {
          group.add(next);
          pending.addLast(next);
        }
      }
    }

    return group;
  }

  private Set<RoomNode> eligible(FloorGraph graph, FloorContext context) {
    Set<RoomNode> free = new LinkedHashSet<>();

    for (RoomNode room : graph.rooms()) {
      if (room.questId() == null && context.types().definition(room.type()).isFiller()) {
        free.add(room);
      }
    }

    return free;
  }
}