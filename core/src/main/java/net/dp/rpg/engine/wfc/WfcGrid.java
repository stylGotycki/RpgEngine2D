package net.dp.rpg.engine.wfc;

import java.util.Arrays;
import java.util.List;

public final class WfcGrid {

  private static final int NOT_FIXED = -1;

  private final int width;

  private final int height;

  private final AdjacencyRules rules;

  private final List<WeightTable> tables;

  private final int[] tableOfCell;

  private final TokenSet[] restrictions;

  private final int[] fixed;

  public WfcGrid(int width, int height, AdjacencyRules rules, List<WeightTable> tables) {
    if (width <= 0 || height <= 0) {
      throw new IllegalArgumentException("Grid dimensions must be greater than zero: %dx%d"
          .formatted(width, height));
    }

    if (rules == null) {
      throw new IllegalArgumentException("Adjacency rules must not be null");
    }

    if (tables == null || tables.isEmpty()) {
      throw new IllegalArgumentException("At least one weight table is required");
    }

    for (WeightTable table : tables) {
      if (table.tokenCount() != rules.tokenCount()) {
        throw new IllegalArgumentException("Weight table covers %d tokens, rules cover %d"
            .formatted(table.tokenCount(), rules.tokenCount()));
      }
    }

    this.width = width;
    this.height = height;
    this.rules = rules;
    this.tables = List.copyOf(tables);
    this.tableOfCell = new int[Math.multiplyExact(width, height)];
    this.restrictions = new TokenSet[tableOfCell.length];
    this.fixed = new int[tableOfCell.length];

    Arrays.fill(fixed, NOT_FIXED);
  }

  public int width() {
    return width;
  }

  public int height() {
    return height;
  }

  public AdjacencyRules rules() {
    return rules;
  }

  public int tokenCount() {
    return rules.tokenCount();
  }

  public boolean isInside(int x, int y) {
    return x >= 0 && x < width && y >= 0 && y < height;
  }

  public WeightTable tableAt(int x, int y) {
    return tables.get(tableOfCell[index(x, y)]);
  }

  public void setTable(int x, int y, int table) {
    if (table < 0 || table >= tables.size()) {
      throw new IllegalArgumentException("Weight table %d is outside 0..%d"
          .formatted(table, tables.size() - 1));
    }

    tableOfCell[index(x, y)] = table;
  }

  public void restrict(int x, int y, TokenSet allowed) {
    if (allowed == null || allowed.tokenCount() != rules.tokenCount()) {
      throw new IllegalArgumentException("Restriction must cover exactly the rule tokens");
    }

    int cell = index(x, y);
    TokenSet current = restrictions[cell];

    restrictions[cell] = current == null ? allowed : current.intersection(allowed);
  }

  public void fix(int x, int y, int token) {
    TokenSet.requireToken(rules.tokenCount(), token);

    fixed[index(x, y)] = token;
  }

  public boolean isFixed(int x, int y) {
    return fixed[index(x, y)] != NOT_FIXED;
  }

  public WfcState newState() {
    return new WfcState(this);
  }

  int cellCount() {
    return tableOfCell.length;
  }

  WeightTable tableOf(int cell) {
    return tables.get(tableOfCell[cell]);
  }

  TokenSet startingDomain(int cell) {
    if (fixed[cell] != NOT_FIXED) {
      return TokenSet.of(rules.tokenCount(), fixed[cell]);
    }

    TokenSet support = tableOf(cell).support();

    return restrictions[cell] == null ? support : support.intersection(restrictions[cell]);
  }

  private int index(int x, int y) {
    if (!isInside(x, y)) {
      throw new IllegalArgumentException("Cell %d,%d is outside the %dx%d grid"
          .formatted(x, y, width, height));
    }

    return x + y * width;
  }
}
