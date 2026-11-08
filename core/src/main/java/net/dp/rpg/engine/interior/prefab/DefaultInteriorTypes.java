package net.dp.rpg.engine.interior.prefab;

import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;

public final class DefaultInteriorTypes {

  private DefaultInteriorTypes() {
  }

  public static Map<RoomType, List<PrefabRequirement>> catalog() {
    List<PrefabRequirement> structures = List.of(
        PrefabRequirement.optional("structure.pillar", 4, 0.3),
        PrefabRequirement.optional("structure.hut", 1, 0.1));

    return Map.of(
        RoomType.SHOP, List.of(PrefabRequirement.required("shop.counter")),
        RoomType.POWER_FIELD, List.of(PrefabRequirement.required("power.core")),
        RoomType.NORMAL, structures,
        RoomType.EMPTY, List.of(PrefabRequirement.optional("structure.pillar", 4, 0.3)));
  }
}
