package net.dp.rpg.engine.floor.phase;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.floor.FloorContext;
import net.dp.rpg.engine.floor.LoopSettings;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.graph.RoomPhase;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class LoopPhase {

  public int open(FloorGraph graph, FloorContext context) {
    LoopSettings settings = context.plan().loops();
    int wanted = settings.loopsFor(context.plan().roomCount());

    if (wanted == 0) {
      return 0;
    }

    Random random = context.stream("loop");
    List<List<RoomEdge>> candidates = candidates(graph);

    Collections.shuffle(candidates, random);
    graph.computeDepths();

    RoomNode boss = graph.rooms().stream()
        .filter(room -> room.type() == RoomType.BOSS)
        .findFirst()
        .orElse(null);
    int bossDepth = boss == null ? -1 : boss.depth();
    int minBossDepth = boss == null ? -1
        : (int) Math.ceil((bossDepth + 1) * (1.0 - settings.maxCriticalLoss())) - 1;
    boolean bossWasDeepest = boss != null && bossDepth >= maxDepthExcept(graph, boss, null);
    int opened = 0;

    while (opened < wanted) {
      List<RoomEdge> best = null;
      int bestCycle = settings.minCycleRooms() - 1;

      for (List<RoomEdge> edges : candidates) {
        RoomNode first = graph.roomAt(edges.getFirst().cell());
        RoomNode second = otherSide(graph, edges.getFirst());

        if (first.isLinkedTo(second)) {
          continue;
        }

        int cycle = distance(graph, first, second) + 1;

        if (cycle > bestCycle && keepsTheBossFar(graph, boss, first, second, minBossDepth,
            bossWasDeepest)) {
          best = edges;
          bestCycle = cycle;
        }
      }

      if (best == null) {
        break;
      }

      RoomEdge edge = best.get(random.nextInt(best.size()));

      graph.connect(graph.roomAt(edge.cell()), otherSide(graph, edge), edge, DoorType.NORMAL);
      opened++;
    }

    graph.computeDepths();

    return opened;
  }

  private static RoomNode otherSide(FloorGraph graph, RoomEdge edge) {
    return graph.roomAt(edge.cell().neighbour(edge.direction()));
  }

  private static boolean keepsTheBossFar(FloorGraph graph, RoomNode boss, RoomNode first,
      RoomNode second, int minBossDepth, boolean bossWasDeepest) {
    if (boss == null) {
      return true;
    }

    int[] depths = depthsWith(graph, first, second);
    int newBossDepth = depths[boss.index()];

    return newBossDepth >= minBossDepth
        && (!bossWasDeepest || newBossDepth >= maxDepthExcept(graph, boss, depths));
  }

  private static List<List<RoomEdge>> candidates(FloorGraph graph) {
    Map<Long, List<RoomEdge>> byPair = new LinkedHashMap<>();

    for (RoomNode room : graph.rooms()) {
      if (room.phase() != RoomPhase.TRUNK) {
        continue;
      }

      for (RoomEdge edge : room.outerEdges()) {
        RoomNode other = graph.roomAt(edge.cell().neighbour(edge.direction()));

        if (other == null || other.index() <= room.index() || other.phase() != RoomPhase.TRUNK
            || room.isLinkedTo(other)) {
          continue;
        }

        long key = (long) room.index() << 32 | other.index();

        byPair.computeIfAbsent(key, ignored -> new ArrayList<>()).add(edge);
      }
    }

    return new ArrayList<>(byPair.values());
  }

  private static int distance(FloorGraph graph, RoomNode from, RoomNode to) {
    int[] steps = bfs(graph, from, null, null);

    return steps[to.index()] < 0 ? Integer.MAX_VALUE - 1 : steps[to.index()];
  }

  private static int[] depthsWith(FloorGraph graph, RoomNode first, RoomNode second) {
    return bfs(graph, graph.start(), first, second);
  }

  private static int[] bfs(FloorGraph graph, RoomNode from, RoomNode extraA, RoomNode extraB) {
    int[] steps = new int[graph.size()];
    Deque<RoomNode> pending = new ArrayDeque<>();

    java.util.Arrays.fill(steps, -1);
    steps[from.index()] = 0;
    pending.add(from);

    while (!pending.isEmpty()) {
      RoomNode current = pending.removeFirst();
      List<RoomNode> next = new ArrayList<>();

      for (RoomLink link : current.links()) {
        next.add(link.other(current));
      }

      if (current == extraA) {
        next.add(extraB);
      } else if (current == extraB) {
        next.add(extraA);
      }

      for (RoomNode neighbour : next) {
        if (steps[neighbour.index()] < 0) {
          steps[neighbour.index()] = steps[current.index()] + 1;
          pending.addLast(neighbour);
        }
      }
    }

    return steps;
  }

  private static int maxDepthExcept(FloorGraph graph, RoomNode skip, int[] depths) {
    int deepest = 0;

    for (RoomNode room : graph.rooms()) {
      if (room != skip) {
        deepest = Math.max(deepest, depths == null ? room.depth() : depths[room.index()]);
      }
    }

    return deepest;
  }
}
