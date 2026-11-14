package net.dp.rpg.engine.interior.prefab;

public record PrefabRequirement(String group, int count, boolean mandatory, double chance) {

  public PrefabRequirement {
    if (group == null || group.isBlank()) {
      throw new IllegalArgumentException("Prefab group must not be blank");
    }

    if (count < 1) {
      throw new IllegalArgumentException("count must be at least 1, got " + count);
    }

    if (!(chance > 0.0 && chance <= 1.0)) {
      throw new IllegalArgumentException("chance must be within (0, 1], got " + chance);
    }

    if (mandatory && chance < 1.0) {
      throw new IllegalArgumentException("A mandatory prefab cannot depend on chance: " + group);
    }
  }

  public static PrefabRequirement required(String group) {
    return new PrefabRequirement(group, 1, true, 1.0);
  }

  public static PrefabRequirement required(String group, int count) {
    return new PrefabRequirement(group, count, true, 1.0);
  }

  public static PrefabRequirement optional(String group) {
    return new PrefabRequirement(group, 1, false, 1.0);
  }

  public static PrefabRequirement optional(String group, int count, double chance) {
    return new PrefabRequirement(group, count, false, chance);
  }
}
