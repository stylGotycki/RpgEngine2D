package net.dp.rpg.game.item;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public final class PlayerStats {

  public static final int POTION_SLOTS = 3;

  public static final float SMALL_DURATION = 20f;

  public static final float BIG_DURATION = 50f;

  public static final float BASE_ATTACK_COOLDOWN = 0.45f;

  private static final double DEFENSE_STEP = 0.06;

  private static final double DEFENSE_CAP = 0.6;

  private static final double AGILITY_ATTACK_SPEED = 0.04;

  private final Map<Stat, Integer> base = new EnumMap<>(Stat.class);

  private final ItemDef[] potions = new ItemDef[POTION_SLOTS];

  private final List<ActiveEffect> effects = new ArrayList<>();

  private ItemDef weapon;

  private ItemDef armor;

  private ItemDef ring;

  public static final class ActiveEffect {

    private final PotionEffect effect;

    private final boolean big;

    private float remaining;

    ActiveEffect(PotionEffect effect, boolean big, float remaining) {
      this.effect = effect;
      this.big = big;
      this.remaining = remaining;
    }

    public PotionEffect effect() {
      return effect;
    }

    public boolean big() {
      return big;
    }

    public float remaining() {
      return remaining;
    }
  }

  public int base(Stat stat) {
    return base.getOrDefault(stat, 0);
  }

  public void setBase(Stat stat, int value) {
    base.put(stat, Math.max(0, value));
  }

  public int get(Stat stat) {
    int value = base(stat);

    for (ItemDef item : equipment()) {
      value += item.bonus(stat);
    }
    for (ActiveEffect active : effects) {
      if (active.effect == PotionEffect.STRENGTH && stat == Stat.STRENGTH
          || active.effect == PotionEffect.WISDOM && stat == Stat.WISDOM) {
        value += active.big ? 4 : 2;
      }
    }

    return Math.max(0, value);
  }

  public WeaponClass weaponClass() {
    return weapon == null ? WeaponClass.FISTS : weapon.weapon();
  }

  public int damage() {
    WeaponClass weaponClass = weaponClass();
    int scaling = weaponClass.scaling() != null ? get(weaponClass.scaling())
        : Math.max(get(Stat.STRENGTH), Math.max(get(Stat.AGILITY), get(Stat.WISDOM)));

    return weaponClass.baseDamage() + 3 * scaling;
  }

  public double attackSpeed() {
    double itemSpeed = weapon == null ? 1.0 : weapon.attackSpeed();

    return weaponClass().attackSpeed() * itemSpeed * (1 + AGILITY_ATTACK_SPEED * get(Stat.AGILITY));
  }

  public float attackCooldown() {
    return (float) (BASE_ATTACK_COOLDOWN / attackSpeed());
  }

  public float moveSpeedMultiplier() {
    return 1f + effect(PotionEffect.SPEED).map(active -> active.big ? 0.6f : 0.3f).orElse(0f);
  }

  public boolean isInvisible() {
    return effect(PotionEffect.INVISIBILITY).isPresent();
  }

  public float damageTaken(float raw) {
    double reduction = Math.min(DEFENSE_CAP, DEFENSE_STEP * get(Stat.DEFENSE));

    return (float) (raw * (1 - reduction));
  }

  // Requirements count the points the player spent, not bonuses: a ring must not unlock a weapon
  // and plate armour must not take one away.
  public Optional<String> missingRequirement(ItemDef item) {
    for (Map.Entry<Stat, Integer> requirement : item.requires().entrySet()) {
      if (base(requirement.getKey()) < requirement.getValue()) {
        return Optional.of(requirement.getKey().shortName() + " " + requirement.getValue());
      }
    }

    return Optional.empty();
  }

  public Optional<ItemDef> equip(ItemDef item) {
    if (!item.isEquipment()) {
      throw new IllegalArgumentException("Not equipment: " + item.id());
    }
    if (missingRequirement(item).isPresent()) {
      throw new IllegalStateException("Requirement not met: " + item.id());
    }

    ItemDef previous = equipped(item.category());

    switch (item.category()) {
      case WEAPON -> weapon = item;
      case ARMOR -> armor = item;
      case RING -> ring = item;
      default -> throw new IllegalStateException();
    }

    return Optional.ofNullable(previous);
  }

  public int damageWith(ItemDef item) {
    ItemDef[] saved = {weapon, armor, ring};

    switch (item.category()) {
      case WEAPON -> weapon = item;
      case ARMOR -> armor = item;
      case RING -> ring = item;
      default -> {
        return damage();
      }
    }

    int damage = damage();
    weapon = saved[0];
    armor = saved[1];
    ring = saved[2];

    return damage;
  }

  public ItemDef equipped(ItemCategory category) {
    return switch (category) {
      case WEAPON -> weapon;
      case ARMOR -> armor;
      case RING -> ring;
      case POTION -> null;
    };
  }

  public List<ItemDef> equipment() {
    List<ItemDef> items = new ArrayList<>();

    for (ItemDef item : new ItemDef[] {weapon, armor, ring}) {
      if (item != null) {
        items.add(item);
      }
    }

    return items;
  }

  public int addPotion(ItemDef potion) {
    if (potion.category() != ItemCategory.POTION) {
      throw new IllegalArgumentException("Not a potion: " + potion.id());
    }

    for (int slot = 0; slot < POTION_SLOTS; slot++) {
      if (potions[slot] == null) {
        potions[slot] = potion;
        return slot;
      }
    }

    return -1;
  }

  public ItemDef potion(int slot) {
    return potions[slot];
  }

  // Healing is returned for the caller to apply to the health component; other effects start,
  // and drinking one already running restarts its timer with the longer of the two durations.
  public float usePotion(int slot) {
    ItemDef potion = potions[slot];

    if (potion == null) {
      return 0;
    }

    potions[slot] = null;

    if (potion.potion() == PotionEffect.HEAL) {
      return potion.big() ? 60 : 25;
    }

    float duration = potion.big() ? BIG_DURATION : SMALL_DURATION;
    Optional<ActiveEffect> running = effect(potion.potion());

    if (running.isPresent() && (running.get().big || running.get().remaining >= duration)) {
      running.get().remaining = Math.max(running.get().remaining, duration);
    } else {
      running.ifPresent(effects::remove);
      effects.add(new ActiveEffect(potion.potion(), potion.big(), duration));
    }

    return 0;
  }

  public void update(float delta) {
    effects.forEach(active -> active.remaining -= delta);
    effects.removeIf(active -> active.remaining <= 0);
  }

  public List<ActiveEffect> effects() {
    return List.copyOf(effects);
  }

  public Optional<ActiveEffect> effect(PotionEffect effect) {
    return effects.stream().filter(active -> active.effect == effect).findFirst();
  }
}
