package net.dp.rpg.engine.floor.io;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.tile.room.RoomCell;

final class FloorDocumentMigration {

  private FloorDocumentMigration() {
  }

  static FloorDocument toCurrent(FloorDocument document, List<String> warnings) {
    if (document.format() == FloorDocument.FORMAT) {
      return document;
    }

    if (document.format() == 1) {
      warnings.add("format 1 (rows counted downwards) converted to format %d"
          .formatted(FloorDocument.FORMAT));

      return fromFormat1(document);
    }

    throw new IllegalArgumentException("Floor format %d is not supported, expected 1 to %d"
        .formatted(document.format(), FloorDocument.FORMAT));
  }

  private static FloorDocument fromFormat1(FloorDocument document) {
    Mirror mirror = Mirror.of(document);
    Map<String, String> anchors = new LinkedHashMap<>();
    List<FloorDocument.RoomDoc> rooms = new ArrayList<>();

    for (FloorDocument.RoomDoc room : document.rooms()) {
      List<RoomCell> cells = new ArrayList<>();

      Cells.read(room.cells()).forEach(cell -> cells.add(mirror.apply(cell)));

      String anchor = Cells.write(cells.stream().min(Cells.ROW_ORDER).orElseThrow());

      anchors.put(room.at(), anchor);
      rooms.add(new FloorDocument.RoomDoc(anchor, Cells.write(cells), room.shape(), room.rot(),
          room.type(), room.phase(), room.quest()));
    }

    rooms.sort(Comparator.comparing(FloorDocument.RoomDoc::at, Mirror::compareCells));

    List<FloorDocument.DoorDoc> doors = new ArrayList<>();

    for (FloorDocument.DoorDoc door : document.doors()) {
      doors.add(new FloorDocument.DoorDoc(Cells.write(mirror.apply(Cells.read(door.at()))),
          door.dir(), door.type()));
    }

    doors.sort(Comparator.comparing(FloorDocument.DoorDoc::at, Mirror::compareCells)
        .thenComparing(FloorDocument.DoorDoc::dir));

    List<FloorDocument.QuestDoc> quests = new ArrayList<>();

    for (FloorDocument.QuestDoc quest : document.quests()) {
      quests.add(new FloorDocument.QuestDoc(quest.id(), renamed(quest.rooms(), anchors)));
    }

    FloorDocument.StateDoc state = document.state() == null
        ? FloorDocument.StateDoc.empty()
        : new FloorDocument.StateDoc(renamed(document.state().visited(), anchors),
            renamed(document.state().cleared(), anchors),
            renamed(document.state().opened(), anchors));

    return new FloorDocument(FloorDocument.FORMAT, document.generator(), document.content(),
        document.archetype(), document.seed(), document.descriptor(), document.attempt(),
        document.settings(), document.preview(), rooms,
        doors, quests, state);
  }

  private static List<String> renamed(List<String> oldAnchors, Map<String, String> anchors) {
    return oldAnchors.stream().map(anchor -> anchors.getOrDefault(anchor, anchor)).toList();
  }

  private record Mirror(int minX, int maxY) {

    static Mirror of(FloorDocument document) {
      int minX = Integer.MAX_VALUE;
      int maxY = Integer.MIN_VALUE;

      for (FloorDocument.RoomDoc room : document.rooms()) {
        for (RoomCell cell : Cells.read(room.cells())) {
          minX = Math.min(minX, cell.x());
          maxY = Math.max(maxY, cell.y());
        }
      }

      return new Mirror(minX, maxY);
    }

    RoomCell apply(RoomCell cell) {
      return new RoomCell(cell.x() - minX, maxY - cell.y());
    }

    static int compareCells(String first, String second) {
      return Cells.ROW_ORDER.compare(Cells.read(first), Cells.read(second));
    }
  }
}
