package net.dp.rpg.engine.floor.type;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public final class Loadout {

  private final Map<AbilityPhase, List<Charge>> charges = new EnumMap<>(AbilityPhase.class);

  private final List<RoomTypeDefinition> fillers;

  private Loadout(List<RoomTypeDefinition> fillers) {
    this.fillers = fillers;

    for (AbilityPhase phase : AbilityPhase.values()) {
      charges.put(phase, new ArrayList<>());
    }
  }

  public static Loadout roll(RoomTypes types, Random random) {
    Loadout loadout = new Loadout(types.fillers());

    for (RoomTypeDefinition definition : types.all()) {
      int count = switch (definition.policy()) {
        case TypePolicy.Exactly exactly -> exactly.count();
        case TypePolicy.AtMost atMost -> atMost.count();
        case TypePolicy.PerFloorChance chance -> random.nextDouble() < chance.chance() ? 1 : 0;
        case TypePolicy.FillerWeight ignored -> 0;
        case TypePolicy.Disabled ignored -> 0;
      };

      if (count > 0) {
        boolean mandatory = definition.policy() instanceof TypePolicy.Exactly
            || definition.policy() instanceof TypePolicy.PerFloorChance;

        loadout.charges.get(definition.phase()).add(new Charge(definition, count, mandatory));
      }
    }

    return loadout;
  }

  public List<Charge> charges(AbilityPhase phase) {
    return List.copyOf(charges.get(phase));
  }

  public boolean hasCharge(AbilityPhase phase, RoomType type) {
    return charges.get(phase).stream().anyMatch(charge -> charge.type() == type && charge.remaining() > 0);
  }

  public RoomType next(AbilityPhase phase, int opportunitiesLeft, Random random) {
    return next(phase, opportunitiesLeft, null, random);
  }

  public RoomType next(AbilityPhase phase, int opportunitiesLeft, RoomType fallback, Random random) {
    List<Charge> pending = new ArrayList<>();

    charges.get(phase).stream()
        .filter(charge -> charge.remaining() > 0 && !structural(charge.definition().slot()))
        .forEach(pending::add);
    pending.sort(Comparator.comparingInt(Charge::remaining).reversed());

    for (Charge charge : pending) {
      double opportunities = Math.max(charge.remaining(), opportunitiesLeft);

      if (random.nextDouble() < charge.remaining() / opportunities) {
        return charge.type();
      }
    }

    return fallback == null ? drawFiller(random) : fallback;
  }

  private static boolean structural(SlotPreference slot) {
    return slot == SlotPreference.CRITICAL_TERMINAL;
  }

  public void spend(AbilityPhase phase, RoomType type) {
    for (Charge charge : charges.get(phase)) {
      if (charge.type() == type && charge.remaining() > 0) {
        charge.remaining--;

        return;
      }
    }
  }

  public RoomType drawFiller(Random random) {
    double total = fillers.stream().mapToDouble(RoomTypeDefinition::fillerWeight).sum();
    double roll = random.nextDouble() * total;

    for (RoomTypeDefinition definition : fillers) {
      roll -= definition.fillerWeight();

      if (roll <= 0.0) {
        return definition.type();
      }
    }

    return fillers.get(fillers.size() - 1).type();
  }

  public List<RoomType> unspentMandatory() {
    List<RoomType> unspent = new ArrayList<>();

    charges.values().forEach(list -> list.stream()
        .filter(charge -> charge.mandatory() && charge.remaining() > 0)
        .forEach(charge -> unspent.add(charge.type())));

    return unspent;
  }

  public static final class Charge {

    private final RoomTypeDefinition definition;

    private final boolean mandatory;

    private int remaining;

    private Charge(RoomTypeDefinition definition, int remaining, boolean mandatory) {
      this.definition = definition;
      this.remaining = remaining;
      this.mandatory = mandatory;
    }

    public RoomTypeDefinition definition() {
      return definition;
    }

    public RoomType type() {
      return definition.type();
    }

    public int remaining() {
      return remaining;
    }

    public boolean mandatory() {
      return mandatory;
    }
  }
}
