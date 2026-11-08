package net.dp.rpg.engine.floor.io;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorDebug;
import net.dp.rpg.engine.floor.FloorDescriptor;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.floor.QuestBundle;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.graph.RoomLink;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.floor.shape.RoomShapeDef;
import net.dp.rpg.engine.floor.shape.ShapePool;
import net.dp.rpg.engine.floor.type.RoomTypeDefinition;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;

public final class FloorDocuments {

  public static final String GENERATOR_VERSION = "0.9.0";

  private FloorDocuments() {
  }

  public static FloorDocument of(FloorLayout floor, FloorArchetype archetype) {
    String content = contentHash(archetype);
    FloorDescriptor descriptor = floor.descriptor() != null ? floor.descriptor()
        : new FloorDescriptor(floor.archetypeId(), floor.seed(), FloorDescriptor.DIRECT,
            FloorSettings.of(archetype));

    return new FloorDocument(
        FloorDocument.FORMAT,
        GENERATOR_VERSION,
        content,
        floor.archetypeId(),
        descriptor.seed(),
        fingerprint(content, descriptor),
        descriptor.attempt(),
        descriptor.settings(),
        preview(floor),
        rooms(floor),
        doors(floor),
        quests(floor),
        FloorDocument.StateDoc.empty());
  }

  public static String contentHash(FloorArchetype archetype) {
    StringBuilder builder = new StringBuilder(archetype.id());

    for (ShapePool.ShapeEntry entry : archetype.defaultShapes().entries()) {
      builder.append('|').append(entry.shape().id()).append('=').append(entry.weight());
    }

    for (RoomTypeDefinition definition : archetype.roomTypes()) {
      builder.append('|').append(definition.type()).append('=').append(definition.policy());
    }

    builder.append('|').append(archetype.quests().roomRatio())
        .append('|').append(archetype.trunkRatio())
        .append('|').append(archetype.cellsPerRoom())
        .append('|').append(archetype.density())
        .append('|').append(archetype.aspect())
        .append('|').append(archetype.criticalShare())
        .append('|').append(archetype.loops())
        .append('|').append(archetype.walker());

    return hex(RandomSource.hash(builder.toString()));
  }

  public static FloorDescriptor descriptorOf(FloorDocument document, FloorArchetype archetype) {
    if (!archetype.id().equals(document.archetype())) {
      throw new IllegalArgumentException("Document is a '%s' floor, not '%s'"
          .formatted(document.archetype(), archetype.id()));
    }

    FloorSettings settings = document.settings() != null ? document.settings()
        : FloorSettings.of(archetype);

    return new FloorDescriptor(document.archetype(), document.seed(), document.attempt(),
        settings);
  }

  private static String fingerprint(String content, FloorDescriptor descriptor) {
    long hash = RandomSource.hash(content + descriptor.archetypeId() + descriptor.settings());

    return hex(RandomSource.mix(hash ^ descriptor.seed()) ^ descriptor.attempt());
  }

  private static String hex(long value) {
    return Long.toHexString(value).substring(0, 8);
  }

  private static List<String> preview(FloorLayout floor) {
    return List.of(FloorDebug.render(floor.graph(), floor.extent()).split("\\R"));
  }

  private static List<FloorDocument.RoomDoc> rooms(FloorLayout floor) {
    List<RoomNode> sorted = new ArrayList<>(floor.graph().rooms());

    sorted.sort(Comparator.comparing(RoomNode::anchor, Cells.ROW_ORDER));

    List<FloorDocument.RoomDoc> docs = new ArrayList<>();

    for (RoomNode room : sorted) {
      docs.add(new FloorDocument.RoomDoc(
          Cells.write(room.anchor()),
          Cells.write(room.cells()),
          room.variant().id(),
          rotationOf(room),
          room.type().name(),
          room.phase().name(),
          room.questId()));
    }

    return docs;
  }

  private static int rotationOf(RoomNode room) {
    RoomShapeDef definition = room.variant().definition();

    return Math.max(0, definition.variants().indexOf(room.variant()));
  }

  private static List<FloorDocument.DoorDoc> doors(FloorLayout floor) {
    List<FloorDocument.DoorDoc> docs = new ArrayList<>();

    for (RoomLink link : floor.graph().links()) {
      RoomEdge edge = canonical(link.edge());

      docs.add(new FloorDocument.DoorDoc(Cells.write(edge.cell()), edge.direction().name(),
          link.doorType().name()));
    }

    docs.sort(Comparator.comparing(FloorDocument.DoorDoc::at, comparingCells())
        .thenComparing(FloorDocument.DoorDoc::dir));

    return docs;
  }

  private static RoomEdge canonical(RoomEdge edge) {
    if (edge.direction() == Direction.EAST || edge.direction() == Direction.NORTH) {
      return edge;
    }

    return new RoomEdge(edge.cell().neighbour(edge.direction()), edge.direction().opposite());
  }

  private static Comparator<String> comparingCells() {
    return (first, second) -> Cells.ROW_ORDER.compare(Cells.read(first), Cells.read(second));
  }

  private static List<FloorDocument.QuestDoc> quests(FloorLayout floor) {
    List<FloorDocument.QuestDoc> docs = new ArrayList<>();

    for (QuestBundle bundle : floor.quests()) {
      List<RoomCell> anchors = new ArrayList<>();

      bundle.rooms().forEach(room -> anchors.add(room.anchor()));

      docs.add(new FloorDocument.QuestDoc(bundle.id(), Cells.write(anchors)));
    }

    docs.sort(Comparator.comparing(FloorDocument.QuestDoc::id));

    return docs;
  }
}
