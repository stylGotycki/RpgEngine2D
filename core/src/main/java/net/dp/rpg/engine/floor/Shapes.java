package net.dp.rpg.engine.floor;

import net.dp.rpg.engine.floor.shape.RoomShapeDef;

import java.util.List;

public final class Shapes {

  public static final String SIGNATURE = "signature";

  public static final RoomShapeDef SINGLE = RoomShapeDef.of("1x1", "#");

  public static final RoomShapeDef WIDE = RoomShapeDef.of("2x1", "##");

  public static final RoomShapeDef CORNER = RoomShapeDef.of("L", "##", "#.");

  public static final RoomShapeDef SQUARE = RoomShapeDef.of("2x2", "##", "##");

  public static final RoomShapeDef JUNCTION = RoomShapeDef.grouped("T", SIGNATURE, "###", ".#.");

  public static final RoomShapeDef HORSESHOE = RoomShapeDef.grouped("U", SIGNATURE, "#.#", "###");

  public static final RoomShapeDef BEAM = RoomShapeDef.grouped("H", SIGNATURE, "#.#", "###", "#.#");

  public static final RoomShapeDef RING = RoomShapeDef.grouped("O", SIGNATURE, "###", "#.#", "###");

  public static final List<RoomShapeDef> ALL =
      List.of(SINGLE, WIDE, CORNER, SQUARE, JUNCTION, HORSESHOE, BEAM, RING);

  private Shapes() {
  }
}
