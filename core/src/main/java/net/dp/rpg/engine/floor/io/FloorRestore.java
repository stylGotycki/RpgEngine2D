package net.dp.rpg.engine.floor.io;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.floor.GridBounds;
import net.dp.rpg.engine.floor.QuestBundle;
import net.dp.rpg.engine.floor.Shapes;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.graph.FloorGraph;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.graph.RoomPhase;
import net.dp.rpg.engine.floor.shape.RoomShapeDef;
import net.dp.rpg.engine.floor.shape.ShapeVariant;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

public final class FloorRestore {

  private FloorRestore() {
  }

  public static Result read(FloorDocument document) {
    if (document.format() > FloorDocument.FORMAT) {
      throw new IllegalArgumentException("Floor format %d is newer than %d"
          .formatted(document.format(), FloorDocument.FORMAT));
    }

    List<String> warnings = new ArrayList<>();

    if (!FloorDocuments.GENERATOR_VERSION.equals(document.generator())) {
      warnings.add("generator %s, file says %s".formatted(FloorDocuments.GENERATOR_VERSION,
          document.generator()));
    }

    FloorGraph graph = new FloorGraph();
    Map<RoomCell, RoomNode> byAnchor = new LinkedHashMap<>();

    for (FloorDocument.RoomDoc room : startFirst(document.rooms())) {
      byAnchor.put(Cells.read(room.at()), place(graph, room, warnings));
    }

    for (FloorDocument.DoorDoc door : document.doors()) {
      connect(graph, door);
    }

    graph.computeDepths();

    return new Result(graph, quests(document, byAnchor, warnings), warnings);
  }

  private static List<FloorDocument.RoomDoc> startFirst(List<FloorDocument.RoomDoc> rooms) {
    List<FloorDocument.RoomDoc> ordered = new ArrayList<>();

    rooms.stream().filter(room -> RoomType.START.name().equals(room.type())).forEach(ordered::add);
    rooms.stream().filter(room -> !RoomType.START.name().equals(room.type())).forEach(ordered::add);

    return ordered;
  }

  private static RoomNode place(FloorGraph graph, FloorDocument.RoomDoc doc, List<String> warnings) {
    List<RoomCell> cells = Cells.read(doc.cells());

    if (cells.isEmpty()) {
      throw new IllegalArgumentException("Room " + doc.at() + " has no cells");
    }

    RoomShape shape = RoomShape.of(cells);
    ShapeVariant variant = variantOf(shape);

    if (variant == null) {
      throw new IllegalArgumentException("Room %s has a shape no catalog entry matches".formatted(doc.at()));
    }

    if (!variant.id().equals(doc.shape())) {
      warnings.add("room %s says shape %s, its cells are %s".formatted(doc.at(), doc.shape(), variant.id()));
    }

    RoomCell origin = originOf(cells);

    if (!graph.canPlace(variant, origin, unlimited())) {
      throw new IllegalArgumentException("Room " + doc.at() + " overlaps ground already taken");
    }

    RoomNode room = graph.place(variant, origin);

    room.setType(typeOf(doc, warnings));
    room.setPhase(phaseOf(doc, warnings));
    room.setQuestId(doc.quest());

    return room;
  }

  private static void connect(FloorGraph graph, FloorDocument.DoorDoc doc) {
    RoomCell cell = Cells.read(doc.at());
    Direction direction = Direction.valueOf(doc.dir());
    RoomNode from = graph.roomAt(cell);
    RoomNode to = graph.roomAt(cell.neighbour(direction));

    if (from == null || to == null) {
      throw new IllegalArgumentException("Door at %s %s has no room on both sides".formatted(doc.at(), doc.dir()));
    }

    graph.connect(from, to, new RoomEdge(cell, direction), DoorType.valueOf(doc.type()));
  }

  private static List<QuestBundle> quests(FloorDocument document, Map<RoomCell, RoomNode> byAnchor,
                                          List<String> warnings) {
    List<QuestBundle> bundles = new ArrayList<>();

    for (FloorDocument.QuestDoc doc : document.quests()) {
      List<RoomNode> rooms = new ArrayList<>();

      for (String anchor : doc.rooms()) {
        RoomNode room = byAnchor.get(Cells.read(anchor));

        if (room == null) {
          warnings.add("quest %s names a room at %s that is not on this floor".formatted(doc.id(), anchor));
        } else {
          rooms.add(room);
        }
      }

      if (!rooms.isEmpty()) {
        bundles.add(new QuestBundle(doc.id(), rooms));
      }
    }

    return bundles;
  }

  private static ShapeVariant variantOf(RoomShape shape) {
    for (RoomShapeDef definition : Shapes.ALL) {
      for (ShapeVariant variant : definition.variants()) {
        if (variant.shape().cells().equals(shape.cells())) {
          return variant;
        }
      }
    }

    return null;
  }

  private static RoomCell originOf(List<RoomCell> cells) {
    int minX = cells.stream().mapToInt(RoomCell::x).min().orElseThrow();
    int minY = cells.stream().mapToInt(RoomCell::y).min().orElseThrow();

    return new RoomCell(minX, minY);
  }

  private static RoomType typeOf(FloorDocument.RoomDoc doc, List<String> warnings) {
    try {
      return RoomType.valueOf(doc.type());
    } catch (IllegalArgumentException exception) {
      warnings.add("room %s has unknown type %s, treated as NORMAL".formatted(doc.at(), doc.type()));

      return RoomType.NORMAL;
    }
  }

  private static RoomPhase phaseOf(FloorDocument.RoomDoc doc, List<String> warnings) {
    try {
      return RoomPhase.valueOf(doc.phase());
    } catch (IllegalArgumentException exception) {
      warnings.add("room %s has unknown phase %s, treated as TRUNK".formatted(doc.at(), doc.phase()));

      return RoomPhase.TRUNK;
    }
  }

  /** A restored floor keeps the coordinates it was saved with, so placement is not bounded here. */
  private static GridBounds unlimited() {
    return new GridBounds(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  public record Result(FloorGraph graph, List<QuestBundle> quests, List<String> warnings) {

    public Result {
      quests = List.copyOf(quests);
      warnings = List.copyOf(warnings);
    }

    public RoomNode boss() {
      return graph.rooms().stream().filter(room -> room.type() == RoomType.BOSS).findFirst().orElse(null);
    }
  }
}