package net.dp.rpg.engine.interior.prefab;

import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.room.Direction;

public record PrefabOrientation(Prefab prefab, Direction facing, boolean flipped, int width,
                                int height, TileGrid ground, TileGrid details, List<TileMapObject> objects) {

  public static List<PrefabOrientation> allFacing(Prefab prefab, Direction drawnFacing,
                                                  Direction facing) {
    List<PrefabOrientation> orientations = new ArrayList<>();

    orientations.add(of(prefab, drawnFacing, facing, false));

    if (prefab.flippable()) {
      orientations.add(of(prefab, drawnFacing, facing, true));
    }

    return orientations;
  }

  private static PrefabOrientation of(Prefab prefab, Direction drawnFacing, Direction facing,
                                      boolean flipped) {
    int turns = Math.floorMod(facing.ordinal() - drawnFacing.ordinal(), 4);
    boolean transposed = turns % 2 == 1;
    int width = transposed ? prefab.height() : prefab.width();
    int height = transposed ? prefab.width() : prefab.height();
    TileGrid ground = transform(prefab.ground(), prefab.width(), prefab.height(), turns, flipped);
    TileGrid details = transform(prefab.details(), prefab.width(), prefab.height(), turns, flipped);
    List<TileMapObject> objects = prefab.objects().stream()
        .map(object -> transformObject(object, prefab.width(), prefab.height(), turns, flipped))
        .toList();

    return new PrefabOrientation(prefab, facing, flipped, width, height, ground, details, objects);
  }

  private static TileGrid transform(TileGrid source, int sourceWidth, int sourceHeight, int turns,
                                    boolean flipped) {
    boolean transposed = turns % 2 == 1;
    int targetWidth = transposed ? sourceHeight : sourceWidth;
    int targetHeight = transposed ? sourceWidth : sourceHeight;
    TileGrid target = new TileGrid(targetWidth, targetHeight);

    for (int y = 0; y < targetHeight; y++) {
      for (int x = 0; x < targetWidth; x++) {
        int rx = flipped ? targetWidth - 1 - x : x;
        int[] source2 = unrotate(rx, y, sourceWidth, sourceHeight, turns);

        target.set(x, y, source.get(source2[0], source2[1]));
      }
    }

    return target;
  }

  private static TileMapObject transformObject(TileMapObject object, int sourceWidth,
                                               int sourceHeight, int turns, boolean flipped) {
    boolean transposed = turns % 2 == 1;
    int targetWidth = transposed ? sourceHeight : sourceWidth;
    float[] rotated = rotateRect(object.x(), object.y(), object.width(), object.height(),
        sourceWidth, sourceHeight, turns);
    float x = rotated[0];
    float y = rotated[1];
    float width = rotated[2];
    float height = rotated[3];

    if (flipped) {
      x = targetWidth - x - width;
    }

    return new TileMapObject(object.id(), object.name(), object.type(), x, y, width, height,
        object.properties());
  }

  private static int[] unrotate(int x, int y, int sourceWidth, int sourceHeight, int turns) {
    return switch (turns) {
      case 0 -> new int[] {x, y};
      case 1 -> new int[] {y, sourceHeight - 1 - x};
      case 2 -> new int[] {sourceWidth - 1 - x, sourceHeight - 1 - y};
      default -> new int[] {sourceWidth - 1 - y, x};
    };
  }

  private static float[] rotateRect(float x, float y, float width, float height, int sourceWidth,
                                    int sourceHeight, int turns) {
    return switch (turns) {
      case 0 -> new float[] {x, y, width, height};
      case 1 -> new float[] {sourceHeight - y - height, x, height, width};
      case 2 -> new float[] {sourceWidth - x - width, sourceHeight - y - height, width, height};
      default -> new float[] {y, sourceWidth - x - width, height, width};
    };
  }
}
