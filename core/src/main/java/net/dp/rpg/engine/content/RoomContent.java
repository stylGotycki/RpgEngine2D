package net.dp.rpg.engine.content;

import java.util.List;
import net.dp.rpg.engine.puzzle.PuzzleSpec;

/** Everything the generator put into one room. */
public record RoomContent(List<ContentSpot> spots, List<PuzzleSpec> puzzles) {

  public static final RoomContent EMPTY = new RoomContent(List.of());

  public RoomContent {
    spots = List.copyOf(spots);
    puzzles = List.copyOf(puzzles);
  }

  public RoomContent(List<ContentSpot> spots) {
    this(spots, List.of());
  }

  public List<ContentSpot> ofKind(ContentKind kind) {
    return spots.stream().filter(spot -> spot.kind() == kind).toList();
  }

  public int count(ContentKind kind) {
    return (int) spots.stream().filter(spot -> spot.kind() == kind).count();
  }
}
