package net.dp.rpg.engine.tile;

import lombok.Getter;
import net.dp.rpg.engine.tile.exception.TileGridBoundsException;

import java.util.Arrays;
import java.util.function.IntFunction;

public final class TileGrid {

  public static final int EMPTY = -1;

  @Getter
  private final int width;

  @Getter
  private final int height;

  private final int[] cells;

  public TileGrid(int width, int height) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Grid dimensions must be greater than zero: %dx%d".formatted(width, height));
    }

    this.width = width;
    this.height = height;
    this.cells = new int[Math.multiplyExact(width, height)];

    Arrays.fill(cells, EMPTY);
  }

  private TileGrid(int width, int height, int[] cells) {
    this.width = width;
    this.height = height;
    this.cells = cells;
  }

  public static TileGrid filled(int width, int height, int runtimeTileId) {
    TileGrid grid = new TileGrid(width, height);

    grid.fill(runtimeTileId);

    return grid;
  }

  public boolean isInside(int x, int y) {
    return x >= 0
        && x < width
        && y >= 0
        && y < height;
  }

  public boolean isEmpty(int x, int y) {
    return get(x, y) == EMPTY;
  }

  public int get(int x, int y) {
    return cells[index(x, y)];
  }

  public int getOrEmpty(int x, int y) {
    return isInside(x, y) ? cells[x + y * width] : EMPTY;
  }

  public void set(int x, int y, int runtimeTileId) {
    validateRuntimeTileId(runtimeTileId);

    cells[index(x, y)] = runtimeTileId;
  }

  public void clear(int x, int y) {
    cells[index(x, y)] = EMPTY;
  }

  public void fill(int runtimeTileId) {
    validateRuntimeTileId(runtimeTileId);

    Arrays.fill(cells, runtimeTileId);
  }

  public void fillRect(int x, int y, int rectWidth, int rectHeight, int runtimeTileId) {
    validateRuntimeTileId(runtimeTileId);

    for (int row = y; row < y + rectHeight; row++) {
      for (int column = x; column < x + rectWidth; column++) {
        if (isInside(column, row)) {
          cells[column + row * width] = runtimeTileId;
        }
      }
    }
  }

  public void blit(TileGrid source, int offsetX, int offsetY, boolean skipEmpty) {
    if (source == null) {
      throw new IllegalArgumentException("Source grid must not be null");
    }

    for (int y = 0; y < source.height; y++) {
      for (int x = 0; x < source.width; x++) {
        int targetX = offsetX + x;
        int targetY = offsetY + y;

        if (!isInside(targetX, targetY)) {
          continue;
        }

        int value = source.cells[x + y * source.width];

        if (skipEmpty && value == EMPTY) {
          continue;
        }

        cells[targetX + targetY * width] = value;
      }
    }
  }

  public TileGrid copy() {
    return new TileGrid(width, height, cells.clone());
  }

  public int size() {
    return cells.length;
  }

  public int countNonEmpty() {
    int count = 0;

    for (int cell : cells) {
      if (cell != EMPTY) {
        count++;
      }
    }

    return count;
  }

  public String toDebugString(IntFunction<String> symbols) {
    StringBuilder builder = new StringBuilder((width + 1) * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int value = cells[x + y * width];
        String symbol = value == EMPTY ? "." : symbols.apply(value);

        builder.append(symbol == null || symbol.isEmpty() ? '?' : symbol.charAt(0));
      }

      builder.append(System.lineSeparator());
    }

    return builder.toString();
  }

  private void validateRuntimeTileId(int runtimeTileId) {
    if (runtimeTileId < EMPTY) {
      throw new IllegalArgumentException("Invalid runtime tile id: " + runtimeTileId);
    }
  }

  private int index(int x, int y) {
    if (!isInside(x, y)) {
      throw new TileGridBoundsException(x, y, width, height);
    }

    return x + y * width;
  }
}
