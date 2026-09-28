package net.dp.rpg.engine.floor.io;

import java.util.List;

public record FloorDocument(
    int format,
    String generator,
    String content,
    String archetype,
    long seed,
    String descriptor,
    List<String> preview,
    List<RoomDoc> rooms,
    List<DoorDoc> doors,
    List<QuestDoc> quests,
    StateDoc state) {

  public static final int FORMAT = 1;

  public FloorDocument {
    preview = List.copyOf(preview);
    rooms = List.copyOf(rooms);
    doors = List.copyOf(doors);
    quests = List.copyOf(quests);
  }

  public record RoomDoc(
      String at,
      List<String> cells,
      String shape,
      int rot,
      String type,
      String phase,
      String quest) {

    public RoomDoc {
      cells = List.copyOf(cells);
    }
  }

  public record DoorDoc(String at, String dir, String type) {
  }

  public record QuestDoc(String id, List<String> rooms) {

    public QuestDoc {
      rooms = List.copyOf(rooms);
    }
  }

  public record StateDoc(List<String> visited, List<String> cleared, List<String> opened) {

    public StateDoc {
      visited = List.copyOf(visited);
      cleared = List.copyOf(cleared);
      opened = List.copyOf(opened);
    }

    public static StateDoc empty() {
      return new StateDoc(List.of(), List.of(), List.of());
    }

    public boolean isEmpty() {
      return visited.isEmpty() && cleared.isEmpty() && opened.isEmpty();
    }
  }
}