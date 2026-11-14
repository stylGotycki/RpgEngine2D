package net.dp.rpg.game.item;

import java.util.Map;

public record ItemDef(
    String id,
    String name,
    ItemCategory category,
    WeaponClass weapon,
    Map<Stat, Integer> bonus,
    Map<Stat, Integer> requires,
    double attackSpeed,
    PotionEffect potion,
    boolean big,
    int tier) {

  public ItemDef {
    if (id == null || id.isBlank() || category == null) {
      throw new IllegalArgumentException("An item needs an id and a category: " + id);
    }
    if (category == ItemCategory.WEAPON && weapon == null) {
      throw new IllegalArgumentException("A weapon needs a weapon class: " + id);
    }
    if (category == ItemCategory.POTION && potion == null) {
      throw new IllegalArgumentException("A potion needs an effect: " + id);
    }

    bonus = Map.copyOf(bonus);
    requires = Map.copyOf(requires);
  }

  public int bonus(Stat stat) {
    return bonus.getOrDefault(stat, 0);
  }

  public boolean isEquipment() {
    return category == ItemCategory.WEAPON || category == ItemCategory.ARMOR
        || category == ItemCategory.RING;
  }

  public String icon() {
    return "items/" + id;
  }
}
