package net.dp.rpg.engine.floor.phase;

import net.dp.rpg.engine.floor.type.AbilityPhase;
import net.dp.rpg.engine.floor.FloorContext;
import net.dp.rpg.engine.floor.type.Loadout;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.floor.type.RoomTypeDefinition;
import net.dp.rpg.engine.floor.type.SlotPreference;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class RetrospectivePass {

  private static final double MIDPOINT_RATIO = 0.60;

  public Result apply(FloorGraph graph, FloorContext context, List<RoomNode> criticalPath) {
    List<RoomType> assigned = new ArrayList<>();
    int relocated = 0;

    for (Loadout.Charge charge : context.loadout().charges(AbilityPhase.RETROSPECTIVE)) {
      int total = charge.remaining();

      // Several rooms of one type spread along the critical path; one keeps the old midpoint.
      for (int index = 0; index < total; index++) {
        RoomNode room = findSlot(graph, context, charge.definition(), criticalPath, index, total);

        if (room == null && index > 0) {
          room = anyFiller(graph, context, criticalPath);
        }

        if (room == null) {
          break;
        }

        room.setType(charge.type());
        context.loadout().spend(AbilityPhase.RETROSPECTIVE, charge.type());
        assigned.add(charge.type());
      }
    }

    for (RoomTypeDefinition definition : context.types().all()) {
      if (definition.relocatable() && relocate(graph, context, definition, criticalPath)) {
        relocated++;
      }
    }

    assigned.addAll(deadEndMinibosses(graph, context, criticalPath));

    return new Result(List.copyOf(assigned), relocated);
  }

  // After the shops took their dead ends: each remaining dead end off the critical path may end
  // its branch with a miniboss. Any shape will do, a branch end is rarely an arena.
  private List<RoomType> deadEndMinibosses(FloorGraph graph, FloorContext context,
                                           List<RoomNode> criticalPath) {
    double chance = context.plan().branches().deadEndMinibosses();
    List<RoomType> placed = new ArrayList<>();

    if (chance <= 0) {
      return placed;
    }

    Random random = context.stream("deadEndMiniboss");

    for (RoomNode room : graph.rooms()) {
      boolean candidate = room.isDeadEnd() && !criticalPath.contains(room)
          && context.types().definition(room.type()).isFiller();

      if (candidate && random.nextDouble() < chance) {
        room.setType(RoomType.MINIBOSS);
        placed.add(RoomType.MINIBOSS);
      }
    }

    return placed;
  }

  // Off the critical path first, so an extra miniboss does not block the way twice.
  private RoomNode anyFiller(FloorGraph graph, FloorContext context, List<RoomNode> criticalPath) {
    RoomNode fallback = null;

    for (RoomNode room : graph.rooms()) {
      if (room.type() == RoomType.START || !context.types().definition(room.type()).isFiller()) {
        continue;
      }

      if (!criticalPath.contains(room)) {
        return room;
      }

      fallback = fallback == null ? room : fallback;
    }

    return fallback;
  }

  private RoomNode findSlot(FloorGraph graph, FloorContext context, RoomTypeDefinition definition,
                            List<RoomNode> criticalPath, int index, int total) {
    double ratio = total <= 1 ? MIDPOINT_RATIO : 0.30 + 0.55 * index / (total - 1);

    return switch (definition.slot()) {
      case CRITICAL_MIDPOINT -> alongCriticalPath(context, definition, criticalPath, ratio);
      case BRANCH_TERMINAL -> bestBranchTerminal(graph, context, definition, criticalPath);
      default -> firstFiller(graph, context, definition);
    };
  }

  private RoomNode alongCriticalPath(FloorContext context, RoomTypeDefinition definition,
                                     List<RoomNode> criticalPath, double ratio) {
    if (criticalPath.size() < 3) {
      return null;
    }

    int target = (int) Math.round(ratio * (criticalPath.size() - 1));

    for (int offset = 0; offset < criticalPath.size(); offset++) {
      for (int step : new int[] {target - offset, target + offset}) {
        if (step <= 0 || step >= criticalPath.size() - 1) {
          continue;
        }

        RoomNode candidate = criticalPath.get(step);

        if (accepts(context, definition, candidate)) {
          return candidate;
        }
      }
    }

    return null;
  }

  private RoomNode bestBranchTerminal(FloorGraph graph, FloorContext context, RoomTypeDefinition definition,
                                      List<RoomNode> criticalPath) {
    RoomNode best = null;
    int bestScore = Integer.MIN_VALUE;

    for (RoomNode room : graph.rooms()) {
      if (!accepts(context, definition, room)) {
        continue;
      }

      int score = branchScore(room, criticalPath);

      if (score > bestScore) {
        bestScore = score;
        best = room;
      }
    }

    return best;
  }

  private RoomNode firstFiller(FloorGraph graph, FloorContext context, RoomTypeDefinition definition) {
    for (RoomNode room : graph.rooms()) {
      if (accepts(context, definition, room)) {
        return room;
      }
    }

    return null;
  }

  private boolean relocate(FloorGraph graph, FloorContext context, RoomTypeDefinition definition,
                           List<RoomNode> criticalPath) {
    RoomNode current = null;

    for (RoomNode room : graph.rooms()) {
      if (room.type() == definition.type()) {
        current = room;
        break;
      }
    }

    if (current == null || definition.slot() != SlotPreference.BRANCH_TERMINAL) {
      return false;
    }

    RoomNode best = bestBranchTerminal(graph, context, definition, criticalPath);

    if (best == null || best == current || branchScore(best, criticalPath) <= branchScore(current, criticalPath)) {
      return false;
    }

    RoomType displaced = best.type();

    best.setType(definition.type());
    current.setType(displaced);

    return true;
  }

  private int branchScore(RoomNode room, List<RoomNode> criticalPath) {
    int score = room.depth();

    if (room.isDeadEnd()) {
      score += 100;
    }

    if (!criticalPath.contains(room)) {
      score += 50;
    }

    return score;
  }

  private boolean accepts(FloorContext context, RoomTypeDefinition definition, RoomNode room) {
    return context.types().definition(room.type()).isFiller()
        && context.types().poolFor(definition.type()).contains(room.variant().definition());
  }

  public record Result(List<RoomType> assigned, int relocated) {
  }
}
