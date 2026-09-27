package net.dp.rpg.engine.floor;

import java.util.List;

public final class DefaultRoomTypes {

  private DefaultRoomTypes() {
  }

  public static List<RoomTypeDefinition> catalog() {
    return List.of(
        new RoomTypeDefinition(RoomType.START, new TypePolicy.Exactly(1), ShapePools.CHAMBER,
            AbilityPhase.TRUNK, SlotPreference.ANY, false),
        new RoomTypeDefinition(RoomType.BOSS, new TypePolicy.Exactly(1), ShapePools.ARENA,
            AbilityPhase.APPENDIX, SlotPreference.CRITICAL_TERMINAL, false),
        new RoomTypeDefinition(RoomType.MINIBOSS, new TypePolicy.PerFloorChance(0.30), ShapePools.ARENA,
            AbilityPhase.RETROSPECTIVE, SlotPreference.CRITICAL_MIDPOINT, false),
        new RoomTypeDefinition(RoomType.SHOP, new TypePolicy.Exactly(1), ShapePools.CHAMBER,
            AbilityPhase.APPENDIX, SlotPreference.BRANCH_TERMINAL, true),
        new RoomTypeDefinition(RoomType.POWER_FIELD, new TypePolicy.Exactly(1), ShapePools.CHAMBER,
            AbilityPhase.TRUNK, SlotPreference.ANY, false),
        new RoomTypeDefinition(RoomType.VAULT, new TypePolicy.Disabled(), ShapePools.CHAMBER,
            AbilityPhase.RETROSPECTIVE, SlotPreference.HOLE, false),
        RoomTypeDefinition.filler(RoomType.NORMAL, 0.70),
        RoomTypeDefinition.filler(RoomType.PUZZLE, 0.20),
        RoomTypeDefinition.filler(RoomType.EMPTY, 0.05),
        RoomTypeDefinition.filler(RoomType.COLLECTIBLE, 0.05));
  }
}
