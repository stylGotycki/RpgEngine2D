package net.dp.rpg.engine.wfc;

public record WfcResult(Status status, int attempts, int observations, WfcState state) {

  public enum Status {

    SOLVED,

    CONTRADICTION,

    UNSATISFIABLE
  }

  public boolean isSolved() {
    return status == Status.SOLVED;
  }

  public int tokenAt(int x, int y) {
    return state.tokenAt(x, y);
  }

  public int[] tokens() {
    int[] tokens = new int[state.width() * state.height()];

    for (int y = 0; y < state.height(); y++) {
      for (int x = 0; x < state.width(); x++) {
        tokens[x + y * state.width()] = state.tokenAt(x, y);
      }
    }

    return tokens;
  }
}
