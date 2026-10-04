package net.dp.rpg.engine.interior.prefab;

public record PrefabRequirement(String group, int count, boolean mandatory) {

  public static PrefabRequirement required(String group) {
    return new PrefabRequirement(group, 1, true);
  }

  public static PrefabRequirement required(String group, int count) {
    return new PrefabRequirement(group, count, true);
  }

  public static PrefabRequirement optional(String group) {
    return new PrefabRequirement(group, 1, false);
  }
}
