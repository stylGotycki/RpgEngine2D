package net.dp.rpg.engine.floor;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.floor.type.AbilityPhase;
import net.dp.rpg.engine.floor.type.DefaultRoomTypes;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.floor.type.RoomTypeDefinition;
import net.dp.rpg.engine.floor.type.SlotPreference;
import net.dp.rpg.engine.floor.type.TypePolicy;

// What a floor is made of: expected shops and minibosses, and the weights of the filler rooms.
// Read from a catalog and written back into it, so the catalog keeps shapes, slots and phases.
public record RoomMix(double shops, double minibosses, double normal, double puzzle,
    double empty, double collectible) {

  public static final RoomMix DEFAULT = of(DefaultRoomTypes.catalog());

  public RoomMix {
    for (double value : new double[] {shops, minibosses, normal, puzzle, empty, collectible}) {
      if (!Double.isFinite(value) || value < 0) {
        throw new IllegalArgumentException("Room mix values must be zero or more: " + value);
      }
    }

    if (normal + puzzle + empty + collectible <= 0) {
      throw new IllegalArgumentException("At least one filler room type needs a weight");
    }
  }

  public static RoomMix of(List<RoomTypeDefinition> catalog) {
    double shops = 0;
    double minibosses = 0;
    double[] fillers = new double[4];

    for (RoomTypeDefinition definition : catalog) {
      switch (definition.type()) {
        case SHOP -> shops = expected(definition.policy());
        case MINIBOSS -> minibosses = expected(definition.policy());
        case NORMAL -> fillers[0] = definition.fillerWeight();
        case PUZZLE -> fillers[1] = definition.fillerWeight();
        case EMPTY -> fillers[2] = definition.fillerWeight();
        case COLLECTIBLE -> fillers[3] = definition.fillerWeight();
        default -> {
        }
      }
    }

    return new RoomMix(shops, minibosses, fillers[0], fillers[1], fillers[2], fillers[3]);
  }

  // inlineShops: shops become trunk rooms, so a floor without branches still has one on the way.
  public List<RoomTypeDefinition> applyTo(List<RoomTypeDefinition> catalog, boolean inlineShops) {
    if (equals(of(catalog)) && !inlineShops) {
      return catalog;
    }

    List<RoomTypeDefinition> result = new ArrayList<>();

    for (RoomTypeDefinition definition : catalog) {
      result.add(switch (definition.type()) {
        case SHOP -> inlineShops
            ? new RoomTypeDefinition(RoomType.SHOP, counted(shops), definition.shapes(),
                AbilityPhase.TRUNK, SlotPreference.ANY, false)
            : with(definition, counted(shops));
        case MINIBOSS -> with(definition, counted(minibosses));
        case NORMAL -> with(definition, new TypePolicy.FillerWeight(normal));
        case PUZZLE -> with(definition, new TypePolicy.FillerWeight(puzzle));
        case EMPTY -> with(definition, new TypePolicy.FillerWeight(empty));
        case COLLECTIBLE -> with(definition, new TypePolicy.FillerWeight(collectible));
        default -> definition;
      });
    }

    return result;
  }

  public RoomMix withShops(double value) {
    return new RoomMix(value, minibosses, normal, puzzle, empty, collectible);
  }

  public RoomMix withMinibosses(double value) {
    return new RoomMix(shops, value, normal, puzzle, empty, collectible);
  }

  public RoomMix withFillers(double normal, double puzzle, double empty, double collectible) {
    return new RoomMix(shops, minibosses, normal, puzzle, empty, collectible);
  }

  private static TypePolicy counted(double count) {
    return count <= 0 ? new TypePolicy.Disabled() : new TypePolicy.Expected(count);
  }

  private static RoomTypeDefinition with(RoomTypeDefinition definition, TypePolicy policy) {
    return new RoomTypeDefinition(definition.type(), policy, definition.shapes(),
        definition.phase(), definition.slot(), definition.relocatable());
  }

  private static double expected(TypePolicy policy) {
    return switch (policy) {
      case TypePolicy.Exactly exactly -> exactly.count();
      case TypePolicy.AtMost atMost -> atMost.count();
      case TypePolicy.PerFloorChance chance -> chance.chance();
      case TypePolicy.Expected expected -> expected.count();
      case TypePolicy.FillerWeight ignored -> 0;
      case TypePolicy.Disabled ignored -> 0;
    };
  }
}
