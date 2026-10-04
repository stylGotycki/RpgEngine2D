package net.dp.rpg.engine.interior.pass;

import java.util.Random;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.tile.TileGrid;

public final class DetailPass {

  private final int detailRuntimeId;

  private final double chance;

  public DetailPass(int detailRuntimeId, double chance) {
    if (chance < 0.0 || chance > 1.0) {
      throw new IllegalArgumentException("Chance must be between 0 and 1: " + chance);
    }

    this.detailRuntimeId = detailRuntimeId;
    this.chance = chance;
  }

  public void apply(RoomCanvas canvas, Random random) {
    for (int y = 0; y < canvas.height(); y++) {
      for (int x = 0; x < canvas.width(); x++) {
        if (canvas.zones().zoneAt(x, y) == Zone.INTERIOR && random.nextDouble() < chance) {
          canvas.setDetail(x, y, detailRuntimeId);
        }
      }
    }
  }
}
