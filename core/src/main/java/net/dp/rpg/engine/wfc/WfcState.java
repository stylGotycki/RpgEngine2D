package net.dp.rpg.engine.wfc;

import java.util.function.IntFunction;
import net.dp.rpg.engine.tile.room.Direction;

public final class WfcState {

  private static final Direction[] DIRECTIONS = Direction.values();

  private static final int NONE = -1;

  private final WfcGrid grid;

  private final AdjacencyRules rules;

  private final int width;

  private final int height;

  private final int words;

  private final long[] domains;

  private final int[] sizes;

  private final double[] entropies;

  private final int[] pending;

  private final boolean[] queued;

  private final long[] mask;

  private int pendingCount;

  private int openCells;

  private int failedCell = NONE;

  WfcState(WfcGrid grid) {
    this.grid = grid;
    this.rules = grid.rules();
    this.width = grid.width();
    this.height = grid.height();
    this.words = rules.words();

    int cells = grid.cellCount();

    this.domains = new long[cells * words];
    this.sizes = new int[cells];
    this.entropies = new double[cells];
    this.pending = new int[cells];
    this.queued = new boolean[cells];
    this.mask = new long[words];

    for (int cell = 0; cell < cells; cell++) {
      TokenSet start = grid.startingDomain(cell);

      for (int index = 0; index < words; index++) {
        domains[cell * words + index] = start.word(index);
      }

      refresh(cell);

      if (sizes[cell] == 0 && failedCell == NONE) {
        failedCell = cell;
      }

      if (sizes[cell] > 1) {
        openCells++;
      }

      enqueue(cell);
    }

    if (failedCell == NONE) {
      drain();
    } else {
      clearPending();
    }
  }

  private WfcState(WfcState source) {
    this.grid = source.grid;
    this.rules = source.rules;
    this.width = source.width;
    this.height = source.height;
    this.words = source.words;
    this.domains = source.domains.clone();
    this.sizes = source.sizes.clone();
    this.entropies = source.entropies.clone();
    this.pending = new int[sizes.length];
    this.queued = new boolean[sizes.length];
    this.mask = new long[words];
    this.openCells = source.openCells;
    this.failedCell = source.failedCell;
  }

  public WfcState copy() {
    return new WfcState(this);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public boolean isConsistent() {
    return failedCell == NONE;
  }

  public boolean isSolved() {
    return failedCell == NONE && openCells == 0;
  }

  public int failedX() {
    return failedCell == NONE ? NONE : failedCell % width;
  }

  public int failedY() {
    return failedCell == NONE ? NONE : failedCell / width;
  }

  public TokenSet domain(int x, int y) {
    return TokenSet.ofWords(rules.tokenCount(), domains, index(x, y) * words);
  }

  public int sizeAt(int x, int y) {
    return sizes[index(x, y)];
  }

  public int tokenAt(int x, int y) {
    int cell = index(x, y);

    return sizes[cell] == 1 ? firstToken(cell) : NONE;
  }

  public boolean restrict(int x, int y, TokenSet allowed) {
    if (allowed == null || allowed.tokenCount() != rules.tokenCount()) {
      throw new IllegalArgumentException("Restriction must cover exactly the rule tokens");
    }

    for (int index = 0; index < words; index++) {
      mask[index] = allowed.word(index);
    }

    return narrowAndPropagate(index(x, y));
  }

  public boolean fix(int x, int y, int token) {
    TokenSet.requireToken(rules.tokenCount(), token);
    singleTokenMask(token);

    return narrowAndPropagate(index(x, y));
  }

  public String toDebugString(IntFunction<String> symbols) {
    StringBuilder builder = new StringBuilder((width + 1) * height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        builder.append(symbolOf(x + y * width, symbols));
      }

      builder.append(System.lineSeparator());
    }

    return builder.toString();
  }

  int cellCount() {
    return sizes.length;
  }

  int openCells() {
    return openCells;
  }

  int size(int cell) {
    return sizes[cell];
  }

  double entropy(int cell) {
    return entropies[cell];
  }

  WeightTable tableOf(int cell) {
    return grid.tableOf(cell);
  }

  int wordCount() {
    return words;
  }

  long word(int cell, int index) {
    return domains[cell * words + index];
  }

  boolean collapse(int cell, int token) {
    singleTokenMask(token);

    return narrowAndPropagate(cell);
  }

  private boolean narrowAndPropagate(int cell) {
    if (failedCell != NONE) {
      return false;
    }

    if (narrow(cell, mask)) {
      if (sizes[cell] == 0) {
        fail(cell);

        return false;
      }

      enqueue(cell);
    }

    return drain();
  }

  private boolean drain() {
    while (pendingCount > 0) {
      int cell = pending[--pendingCount];
      int x = cell % width;
      int y = cell / width;

      queued[cell] = false;

      for (Direction direction : DIRECTIONS) {
        int neighbourX = x + direction.getDeltaX();
        int neighbourY = y + direction.getDeltaY();

        if (neighbourX < 0 || neighbourX >= width || neighbourY < 0 || neighbourY >= height) {
          continue;
        }

        int neighbour = neighbourX + neighbourY * width;

        gatherNeighbours(cell, direction.ordinal(), mask);

        if (narrow(neighbour, mask)) {
          if (sizes[neighbour] == 0) {
            fail(neighbour);

            return false;
          }

          enqueue(neighbour);
        }
      }
    }

    return true;
  }

  private void gatherNeighbours(int cell, int direction, long[] target) {
    for (int index = 0; index < words; index++) {
      target[index] = 0L;
    }

    int base = cell * words;

    for (int index = 0; index < words; index++) {
      long bits = domains[base + index];

      while (bits != 0) {
        rules.orNeighbours((index << 6) + Long.numberOfTrailingZeros(bits), direction, target);
        bits &= bits - 1;
      }
    }
  }

  private boolean narrow(int cell, long[] allowed) {
    int base = cell * words;
    boolean changed = false;

    for (int index = 0; index < words; index++) {
      long narrowed = domains[base + index] & allowed[index];

      if (narrowed != domains[base + index]) {
        domains[base + index] = narrowed;
        changed = true;
      }
    }

    if (changed) {
      boolean wasOpen = sizes[cell] > 1;

      refresh(cell);

      if (wasOpen && sizes[cell] <= 1) {
        openCells--;
      }
    }

    return changed;
  }

  private void refresh(int cell) {
    int base = cell * words;
    int size = 0;

    for (int index = 0; index < words; index++) {
      size += Long.bitCount(domains[base + index]);
    }

    sizes[cell] = size;

    if (size <= 1) {
      entropies[cell] = 0.0;

      return;
    }

    WeightTable table = grid.tableOf(cell);
    double sum = 0.0;
    double sumOfLogs = 0.0;

    for (int index = 0; index < words; index++) {
      long bits = domains[base + index];

      while (bits != 0) {
        int token = (index << 6) + Long.numberOfTrailingZeros(bits);

        sum += table.weight(token);
        sumOfLogs += table.weightLogWeight(token);
        bits &= bits - 1;
      }
    }

    entropies[cell] = sum > 0.0 ? StrictMath.log(sum) - sumOfLogs / sum : 0.0;
  }

  private void singleTokenMask(int token) {
    for (int index = 0; index < words; index++) {
      mask[index] = 0L;
    }

    mask[token >>> 6] = 1L << token;
  }

  private void enqueue(int cell) {
    if (!queued[cell]) {
      queued[cell] = true;
      pending[pendingCount++] = cell;
    }
  }

  private void fail(int cell) {
    failedCell = cell;
    clearPending();
  }

  private void clearPending() {
    while (pendingCount > 0) {
      queued[pending[--pendingCount]] = false;
    }
  }

  private int firstToken(int cell) {
    int base = cell * words;

    for (int index = 0; index < words; index++) {
      if (domains[base + index] != 0) {
        return (index << 6) + Long.numberOfTrailingZeros(domains[base + index]);
      }
    }

    return NONE;
  }

  private char symbolOf(int cell, IntFunction<String> symbols) {
    int size = sizes[cell];

    if (size == 0) {
      return '!';
    }

    if (size == 1) {
      String symbol = symbols.apply(firstToken(cell));

      return symbol == null || symbol.isEmpty() ? '?' : symbol.charAt(0);
    }

    return size < 10 ? (char) ('0' + size) : '+';
  }

  private int index(int x, int y) {
    if (x < 0 || x >= width || y < 0 || y >= height) {
      throw new IllegalArgumentException("Cell %d,%d is outside the %dx%d grid"
          .formatted(x, y, width, height));
    }

    return x + y * width;
  }
}
