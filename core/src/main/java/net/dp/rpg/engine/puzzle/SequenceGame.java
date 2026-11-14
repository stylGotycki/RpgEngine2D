package net.dp.rpg.engine.puzzle;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Simon says: a new random sequence on every start, never the same plate twice in a row; one
// wrong plate ends the attempt.
public final class SequenceGame {

  public enum Result {
    IGNORED,
    CORRECT,
    WRONG,
    SOLVED
  }

  private final int plates;

  private final int length;

  private final List<Integer> sequence = new ArrayList<>();

  private int position;

  private boolean solved;

  public SequenceGame(int plates, int length) {
    this.plates = plates;
    this.length = length;
  }

  public List<Integer> start(Random random) {
    sequence.clear();
    position = 0;

    int previous = -1;
    for (int i = 0; i < length; i++) {
      int plate;
      do {
        plate = random.nextInt(plates);
      } while (plate == previous);
      sequence.add(plate);
      previous = plate;
    }

    return List.copyOf(sequence);
  }

  public boolean isRunning() {
    return !sequence.isEmpty() && !solved;
  }

  public Result step(int plate) {
    if (!isRunning()) {
      return Result.IGNORED;
    }
    if (sequence.get(position) != plate) {
      sequence.clear();
      return Result.WRONG;
    }

    position++;
    if (position == sequence.size()) {
      solved = true;
      return Result.SOLVED;
    }

    return Result.CORRECT;
  }

  public boolean isSolved() {
    return solved;
  }

  public int plates() {
    return plates;
  }

  public int progress() {
    return position;
  }

  public int length() {
    return length;
  }
}
