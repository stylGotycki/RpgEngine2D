package net.dp.rpg.game.item;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import net.dp.rpg.engine.content.ContentTags;

public final class LootTable {

  private static final double UNIQUE_WEIGHT = 0.03;

  private final ItemRegistry items;

  public LootTable(ItemRegistry items) {
    this.items = items;
  }

  public ItemDef resolve(String tag, Random random, double depth) {
    return switch (tag) {
      case ContentTags.EQUIPMENT -> equipment(random, depth, List.of());
      case ContentTags.POTION -> potion(random, depth);
      case ContentTags.LOOT -> random.nextBoolean() ? equipment(random, depth, List.of())
          : potion(random, depth);
      default -> throw new IllegalArgumentException("Not an item pool: " + tag);
    };
  }

  public List<ItemDef> shopOffers(int count, Random random, double depth) {
    List<ItemDef> offers = new ArrayList<>();

    for (int i = 0; i < count - 1; i++) {
      offers.add(equipment(random, depth, offers));
    }
    if (count > 0) {
      offers.add(potion(random, depth));
    }

    return offers;
  }

  // Shallow rooms favour tier 1, the deepest tier 3; the unique weapon is always rare.
  private ItemDef equipment(Random random, double depth, List<ItemDef> exclude) {
    List<ItemDef> pool = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    double peak = 1 + 2 * depth;

    for (ItemDef item : items.all()) {
      if (!item.isEquipment() || exclude.contains(item)) {
        continue;
      }

      pool.add(item);
      weights.add(item.weapon() == WeaponClass.UNIQUE ? UNIQUE_WEIGHT
          : Math.exp(-Math.pow(item.tier() - peak, 2)));
    }

    return pick(pool, weights, random);
  }

  private ItemDef potion(Random random, double depth) {
    PotionEffect effect = PotionEffect.values()[random.nextInt(PotionEffect.values().length)];
    boolean big = random.nextDouble() < 0.3 + 0.3 * depth;

    return items.of(ItemCategory.POTION).stream()
        .filter(item -> item.potion() == effect && item.big() == big)
        .findFirst()
        .orElseThrow(() -> new IllegalStateException("No potion " + effect + " big=" + big));
  }

  private static ItemDef pick(List<ItemDef> pool, List<Double> weights, Random random) {
    double total = weights.stream().mapToDouble(Double::doubleValue).sum();
    double roll = random.nextDouble() * total;

    for (int i = 0; i < pool.size(); i++) {
      roll -= weights.get(i);

      if (roll < 0) {
        return pool.get(i);
      }
    }

    return pool.get(pool.size() - 1);
  }
}
