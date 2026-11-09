package net.dp.rpg.engine.content;

import java.util.List;

/** Everything the generator put into one room. */
public record RoomContent(List<ContentSpot> spots) {

  public static final RoomContent EMPTY = new RoomContent(List.of());

  public RoomContent {
    spots = List.copyOf(spots);
  }

  public List<ContentSpot> ofKind(ContentKind kind) {
    return spots.stream().filter(spot -> spot.kind() == kind).toList();
  }

  public int count(ContentKind kind) {
    return (int) spots.stream().filter(spot -> spot.kind() == kind).count();
  }
}
