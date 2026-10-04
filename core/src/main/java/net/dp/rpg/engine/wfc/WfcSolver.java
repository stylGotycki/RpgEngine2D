package net.dp.rpg.engine.wfc;

import java.util.Random;
import java.util.function.IntFunction;

public final class WfcSolver {

  static final double TIE_NOISE = 1e-6;

  private final int maxAttempts;

  public WfcSolver(int maxAttempts) {
    if (maxAttempts <= 0) {
      throw new IllegalArgumentException("Attempt limit must be greater than zero: " + maxAttempts);
    }

    this.maxAttempts = maxAttempts;
  }

  public int maxAttempts() {
    return maxAttempts;
  }

  public WfcResult solve(WfcGrid grid, IntFunction<Random> randomForAttempt) {
    if (grid == null || randomForAttempt == null) {
      throw new IllegalArgumentException("Grid and random streams must not be null");
    }

    WfcState start = grid.newState();

    if (!start.isConsistent()) {
      return new WfcResult(WfcResult.Status.UNSATISFIABLE, 0, 0, start);
    }

    WfcState last = start;
    int observations = 0;

    for (int attempt = 0; attempt < maxAttempts; attempt++) {
      WfcState state = start.copy();

      observations = run(state, randomForAttempt.apply(attempt));

      if (state.isSolved()) {
        return new WfcResult(WfcResult.Status.SOLVED, attempt + 1, observations, state);
      }

      last = state;
    }

    return new WfcResult(WfcResult.Status.CONTRADICTION, maxAttempts, observations, last);
  }

  /** Runs one attempt on the given state in place and returns the number of observations. */
  public int run(WfcState state, Random random) {
    double[] noise = new double[state.cellCount()];

    for (int cell = 0; cell < noise.length; cell++) {
      noise[cell] = random.nextDouble() * TIE_NOISE;
    }

    int observations = 0;

    while (state.isConsistent() && state.openCells() > 0) {
      int cell = lowestEntropy(state, noise);

      state.collapse(cell, chooseToken(state, cell, random));
      observations++;
    }

    return observations;
  }

  private static int lowestEntropy(WfcState state, double[] noise) {
    int best = -1;
    double bestScore = Double.POSITIVE_INFINITY;

    for (int cell = 0; cell < noise.length; cell++) {
      if (state.size(cell) > 1) {
        double score = state.entropy(cell) + noise[cell];

        if (score < bestScore) {
          bestScore = score;
          best = cell;
        }
      }
    }

    return best;
  }

  private static int chooseToken(WfcState state, int cell, Random random) {
    WeightTable table = state.tableOf(cell);
    double total = 0.0;

    for (int index = 0; index < state.wordCount(); index++) {
      long bits = state.word(cell, index);

      while (bits != 0) {
        total += table.weight((index << 6) + Long.numberOfTrailingZeros(bits));
        bits &= bits - 1;
      }
    }

    double target = random.nextDouble() * total;
    int chosen = -1;

    for (int index = 0; index < state.wordCount(); index++) {
      long bits = state.word(cell, index);

      while (bits != 0) {
        chosen = (index << 6) + Long.numberOfTrailingZeros(bits);
        target -= table.weight(chosen);

        if (target < 0.0) {
          return chosen;
        }

        bits &= bits - 1;
      }
    }

    return chosen;
  }
}
