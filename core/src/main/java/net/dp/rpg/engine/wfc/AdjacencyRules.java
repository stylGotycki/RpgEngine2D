package net.dp.rpg.engine.wfc;

import net.dp.rpg.engine.tile.room.Direction;

public final class AdjacencyRules {

  private static final int DIRECTIONS = Direction.values().length;

  private final int tokenCount;

  private final int words;

  private final long[] compatible;

  private AdjacencyRules(int tokenCount, long[] compatible) {
    this.tokenCount = tokenCount;
    this.words = TokenSet.wordCount(tokenCount);
    this.compatible = compatible;
  }

  public static Builder builder(int tokenCount) {
    return new Builder(tokenCount);
  }

  public int tokenCount() {
    return tokenCount;
  }

  public boolean allows(int token, Direction direction, int neighbour) {
    TokenSet.requireToken(tokenCount, token);
    TokenSet.requireToken(tokenCount, neighbour);

    return (compatible[offset(token, direction.ordinal()) + (neighbour >>> 6)]
        & 1L << neighbour) != 0;
  }

  public TokenSet neighbours(int token, Direction direction) {
    TokenSet.requireToken(tokenCount, token);

    return TokenSet.ofWords(tokenCount, compatible, offset(token, direction.ordinal()));
  }

  public int pairCount() {
    int pairs = 0;

    for (long word : compatible) {
      pairs += Long.bitCount(word);
    }

    return pairs;
  }

  int words() {
    return words;
  }

  void orNeighbours(int token, int direction, long[] target) {
    int base = offset(token, direction);

    for (int index = 0; index < words; index++) {
      target[index] |= compatible[base + index];
    }
  }

  private int offset(int token, int direction) {
    return (direction * tokenCount + token) * words;
  }

  public static final class Builder {

    private final int tokenCount;

    private final int words;

    private final long[] compatible;

    private Builder(int tokenCount) {
      if (tokenCount <= 0) {
        throw new IllegalArgumentException("Token count must be greater than zero: " + tokenCount);
      }

      this.tokenCount = tokenCount;
      this.words = TokenSet.wordCount(tokenCount);
      this.compatible = new long[DIRECTIONS * tokenCount * words];
    }

    public Builder allow(int token, Direction direction, int neighbour) {
      TokenSet.requireToken(tokenCount, token);
      TokenSet.requireToken(tokenCount, neighbour);

      set(token, direction.ordinal(), neighbour);
      set(neighbour, direction.opposite().ordinal(), token);

      return this;
    }

    public Builder allowEverywhere(int token, int neighbour) {
      for (Direction direction : Direction.values()) {
        allow(token, direction, neighbour);
      }

      return this;
    }

    public AdjacencyRules build() {
      return new AdjacencyRules(tokenCount, compatible.clone());
    }

    private void set(int token, int direction, int neighbour) {
      compatible[(direction * tokenCount + token) * words + (neighbour >>> 6)] |= 1L << neighbour;
    }
  }
}
