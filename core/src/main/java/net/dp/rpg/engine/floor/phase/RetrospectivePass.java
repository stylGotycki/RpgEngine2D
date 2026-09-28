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

public final class RetrospectivePass {

  private static final double MIDPOINT_RATIO = 0.60;

  public Result apply(FloorGraph graph, FloorContext context, List<RoomNode> criticalPath) {
    List<RoomType> assigned = new ArrayList<>();
    int relocated = 0;

    for (Loadout.Charge charge : context.loadout().charges(AbilityPhase.RETROSPECTIVE)) {
      if (charge.remaining() <= 0) {
        continue;
      }

      RoomNode room = findSlot(graph, context, charge.definition(), criticalPath);

      if (room != null) {
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

    return new Result(List.copyOf(assigned), relocated);
  }

  private RoomNode findSlot(FloorGraph graph, FloorContext context, RoomTypeDefinition definition,
                            List<RoomNode> criticalPath) {
    return switch (definition.slot()) {
      case CRITICAL_MIDPOINT -> alongCriticalPath(context, definition, criticalPath);
      case BRANCH_TERMINAL -> bestBranchTerminal(graph, context, definition, criticalPath);
      default -> firstFiller(graph, context, definition);
    };
  }

  private RoomNode alongCriticalPath(FloorContext context, RoomTypeDefinition definition,
                                     List<RoomNode> criticalPath) {
    if (criticalPath.size() < 3) {
      return null;
    }

    int target = (int) Math.round(MIDPOINT_RATIO * (criticalPath.size() - 1));

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
