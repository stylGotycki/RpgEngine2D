package net.dp.rpg.engine.interior.model;

import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.wfc.AdjacencyRules;
import net.dp.rpg.engine.wfc.WeightTable;

public final class InteriorModel {

  private final TileClasses classes;

  private final AdjacencyRules rules;

  private final Map<RoomType, Map<Zone, WeightTable>> weightsByTypeAndZone;

  InteriorModel(TileClasses classes, AdjacencyRules rules,
                Map<RoomType, Map<Zone, WeightTable>> weightsByTypeAndZone) {
    this.classes = classes;
    this.rules = rules;
    this.weightsByTypeAndZone = copy(weightsByTypeAndZone);
  }

  public TileClasses classes() {
    return classes;
  }

  public AdjacencyRules rules() {
    return rules;
  }

  public WeightTable weightsOf(RoomType type, Zone zone) {
    if (zone == Zone.VOID) {
      throw new IllegalArgumentException("VOID tiles are not generated, they have no weight table");
    }

    WeightTable table = weightsByTypeAndZone.get(type).get(zone);

    if (table == null) {
      throw new IllegalArgumentException("No learned weights for room type %s in zone %s; the corpus has no sample of this zone"
              .formatted(type, zone));
    }

    return table;
  }

  public boolean hasWeights(RoomType type, Zone zone) {
    Map<Zone, WeightTable> byZone = weightsByTypeAndZone.get(type);

    return byZone != null && byZone.containsKey(zone);
  }

  private static Map<RoomType, Map<Zone, WeightTable>> copy(
      Map<RoomType, Map<Zone, WeightTable>> source) {
    Map<RoomType, Map<Zone, WeightTable>> copy = new EnumMap<>(RoomType.class);

    source.forEach((type, byZone) -> copy.put(type, new EnumMap<>(byZone)));

    return copy;
  }
}
