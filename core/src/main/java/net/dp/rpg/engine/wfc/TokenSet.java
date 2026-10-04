package net.dp.rpg.engine.wfc;

import java.util.Arrays;

public final class TokenSet {

  private final int tokenCount;

  private final long[] words;

  private TokenSet(int tokenCount, long[] words) {
    this.tokenCount = tokenCount;
    this.words = words;
  }

  public static TokenSet none(int tokenCount) {
    return new TokenSet(requireTokenCount(tokenCount), new long[wordCount(tokenCount)]);
  }

  public static TokenSet all(int tokenCount) {
    long[] words = new long[wordCount(requireTokenCount(tokenCount))];

    Arrays.fill(words, -1L);
    clearTail(tokenCount, words);

    return new TokenSet(tokenCount, words);
  }

  public static TokenSet of(int tokenCount, int... tokens) {
    long[] words = new long[wordCount(requireTokenCount(tokenCount))];

    for (int token : tokens) {
      requireToken(tokenCount, token);
      words[token >>> 6] |= 1L << token;
    }

    return new TokenSet(tokenCount, words);
  }

  public static TokenSet of(int tokenCount, Iterable<Integer> tokens) {
    long[] words = new long[wordCount(requireTokenCount(tokenCount))];

    for (int token : tokens) {
      requireToken(tokenCount, token);
      words[token >>> 6] |= 1L << token;
    }

    return new TokenSet(tokenCount, words);
  }

  public int tokenCount() {
    return tokenCount;
  }

  public boolean contains(int token) {
    return token >= 0 && token < tokenCount && (words[token >>> 6] & 1L << token) != 0;
  }

  public int size() {
    int size = 0;

    for (long word : words) {
      size += Long.bitCount(word);
    }

    return size;
  }

  public boolean isEmpty() {
    for (long word : words) {
      if (word != 0) {
        return false;
      }
    }

    return true;
  }

  public int first() {
    for (int index = 0; index < words.length; index++) {
      if (words[index] != 0) {
        return (index << 6) + Long.numberOfTrailingZeros(words[index]);
      }
    }

    return -1;
  }

  public TokenSet union(TokenSet other) {
    requireSameCount(other);

    long[] result = words.clone();

    for (int index = 0; index < result.length; index++) {
      result[index] |= other.words[index];
    }

    return new TokenSet(tokenCount, result);
  }

  public TokenSet intersection(TokenSet other) {
    requireSameCount(other);

    long[] result = words.clone();

    for (int index = 0; index < result.length; index++) {
      result[index] &= other.words[index];
    }

    return new TokenSet(tokenCount, result);
  }

  public TokenSet minus(TokenSet other) {
    requireSameCount(other);

    long[] result = words.clone();

    for (int index = 0; index < result.length; index++) {
      result[index] &= ~other.words[index];
    }

    return new TokenSet(tokenCount, result);
  }

  public int[] toArray() {
    int[] tokens = new int[size()];
    int next = 0;

    for (int index = 0; index < words.length; index++) {
      long bits = words[index];

      while (bits != 0) {
        tokens[next++] = (index << 6) + Long.numberOfTrailingZeros(bits);
        bits &= bits - 1;
      }
    }

    return tokens;
  }

  @Override
  public boolean equals(Object other) {
    return other instanceof TokenSet set
        && set.tokenCount == tokenCount
        && Arrays.equals(set.words, words);
  }

  @Override
  public int hashCode() {
    return 31 * tokenCount + Arrays.hashCode(words);
  }

  @Override
  public String toString() {
    return Arrays.toString(toArray());
  }

  static int wordCount(int tokenCount) {
    return (tokenCount + 63) >>> 6;
  }

  static TokenSet ofWords(int tokenCount, long[] source, int offset) {
    long[] words = Arrays.copyOfRange(source, offset, offset + wordCount(tokenCount));

    return new TokenSet(tokenCount, words);
  }

  long word(int index) {
    return words[index];
  }

  private static void clearTail(int tokenCount, long[] words) {
    int tail = tokenCount & 63;

    if (tail != 0) {
      words[words.length - 1] &= (1L << tail) - 1;
    }
  }

  private void requireSameCount(TokenSet other) {
    if (other.tokenCount != tokenCount) {
      throw new IllegalArgumentException("Token sets of different sizes: %d and %d"
          .formatted(tokenCount, other.tokenCount));
    }
  }

  private static int requireTokenCount(int tokenCount) {
    if (tokenCount <= 0) {
      throw new IllegalArgumentException("Token count must be greater than zero: " + tokenCount);
    }

    return tokenCount;
  }

  static void requireToken(int tokenCount, int token) {
    if (token < 0 || token >= tokenCount) {
      throw new IllegalArgumentException("Token %d is outside 0..%d"
          .formatted(token, tokenCount - 1));
    }
  }
}
