package net.dp.rpg.engine.floor.io;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import com.badlogic.gdx.utils.JsonWriter;
import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.floor.FloorDescriptor;
import net.dp.rpg.engine.floor.BranchSettings;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.floor.QuestSettings;
import net.dp.rpg.engine.floor.RoomMix;
import net.dp.rpg.engine.floor.LoopSettings;

public final class FloorJson {

  private static final int SINGLE_LINE_COLUMNS = 160;

  private FloorJson() {
  }

  public static String write(FloorDocument document) {
    JsonValue root = object();

    root.addChild("format", new JsonValue((long) document.format()));
    root.addChild("generator", new JsonValue(document.generator()));
    root.addChild("content", new JsonValue(document.content()));
    root.addChild("archetype", new JsonValue(document.archetype()));
    root.addChild("seed", new JsonValue(document.seed()));
    root.addChild("descriptor", new JsonValue(document.descriptor()));
    root.addChild("attempt", new JsonValue((long) document.attempt()));

    if (document.settings() != null) {
      root.addChild("settings", settings(document.settings()));
    }
    root.addChild("preview", strings(document.preview()));

    JsonValue rooms = array();

    for (FloorDocument.RoomDoc room : document.rooms()) {
      JsonValue node = object();

      node.addChild("at", new JsonValue(room.at()));
      node.addChild("cells", strings(room.cells()));
      node.addChild("shape", new JsonValue(room.shape()));
      node.addChild("rot", new JsonValue((long) room.rot()));
      node.addChild("type", new JsonValue(room.type()));
      node.addChild("phase", new JsonValue(room.phase()));

      if (room.quest() != null) {
        node.addChild("quest", new JsonValue(room.quest()));
      }

      rooms.addChild(node);
    }

    root.addChild("rooms", rooms);

    JsonValue doors = array();

    for (FloorDocument.DoorDoc door : document.doors()) {
      JsonValue node = object();

      node.addChild("at", new JsonValue(door.at()));
      node.addChild("dir", new JsonValue(door.dir()));
      node.addChild("type", new JsonValue(door.type()));
      doors.addChild(node);
    }

    root.addChild("doors", doors);

    JsonValue quests = array();

    for (FloorDocument.QuestDoc quest : document.quests()) {
      JsonValue node = object();

      node.addChild("id", new JsonValue(quest.id()));
      node.addChild("rooms", strings(quest.rooms()));
      quests.addChild(node);
    }

    root.addChild("quests", quests);

    if (!document.state().isEmpty()) {
      JsonValue state = object();

      state.addChild("visited", strings(document.state().visited()));
      state.addChild("cleared", strings(document.state().cleared()));
      state.addChild("opened", strings(document.state().opened()));
      root.addChild("state", state);
    }

    return root.prettyPrint(JsonWriter.OutputType.json, SINGLE_LINE_COLUMNS);
  }

  public static FloorDocument read(String text) {
    JsonValue root = new JsonReader().parse(text);

    if (root == null) {
      throw new IllegalArgumentException("Floor document is empty");
    }

    List<FloorDocument.RoomDoc> rooms = new ArrayList<>();

    for (JsonValue node = child(root, "rooms"); node != null; node = node.next) {
      rooms.add(new FloorDocument.RoomDoc(
          node.getString("at"),
          strings(node.get("cells")),
          node.getString("shape", "1x1"),
          node.getInt("rot", 0),
          node.getString("type", "NORMAL"),
          node.getString("phase", "TRUNK"),
          node.getString("quest", null)));
    }

    List<FloorDocument.DoorDoc> doors = new ArrayList<>();

    for (JsonValue node = child(root, "doors"); node != null; node = node.next) {
      doors.add(new FloorDocument.DoorDoc(node.getString("at"), node.getString("dir"),
          node.getString("type", "NORMAL")));
    }

    List<FloorDocument.QuestDoc> quests = new ArrayList<>();

    for (JsonValue node = child(root, "quests"); node != null; node = node.next) {
      quests.add(new FloorDocument.QuestDoc(node.getString("id"), strings(node.get("rooms"))));
    }

    return new FloorDocument(
        root.getInt("format", FloorDocument.FORMAT),
        root.getString("generator", "unknown"),
        root.getString("content", ""),
        root.getString("archetype", "default"),
        root.getLong("seed", 0L),
        root.getString("descriptor", ""),
        root.getInt("attempt", FloorDescriptor.DIRECT),
        settings(root.get("settings")),
        strings(root.get("preview")),
        rooms,
        doors,
        quests,
        state(root.get("state")));
  }

  private static JsonValue settings(FloorSettings settings) {
    JsonValue node = object();
    JsonValue loops = object();

    node.addChild("minRooms", new JsonValue((long) settings.minRooms()));
    node.addChild("maxRooms", new JsonValue((long) settings.maxRooms()));
    node.addChild("trunkRatio", new JsonValue(settings.trunkRatio()));
    node.addChild("criticalShare", new JsonValue(settings.criticalShare()));
    loops.addChild("perRoom", new JsonValue(settings.loops().loopsPerRoom()));
    loops.addChild("minCycleRooms", new JsonValue((long) settings.loops().minCycleRooms()));
    loops.addChild("maxCriticalLoss", new JsonValue(settings.loops().maxCriticalLoss()));
    node.addChild("loops", loops);
    node.addChild("questBundleMax", new JsonValue((long) settings.questBundleMax()));
    node.addChild("linearity", new JsonValue(settings.linearity()));
    JsonValue branches = object();
    branches.addChild("chaining", new JsonValue(settings.branches().chaining()));
    branches.addChild("deadEndMinibosses",
        new JsonValue(settings.branches().deadEndMinibosses()));
    node.addChild("branches", branches);
    JsonValue rooms = object();
    rooms.addChild("shops", new JsonValue(settings.rooms().shops()));
    rooms.addChild("minibosses", new JsonValue(settings.rooms().minibosses()));
    rooms.addChild("normal", new JsonValue(settings.rooms().normal()));
    rooms.addChild("puzzle", new JsonValue(settings.rooms().puzzle()));
    rooms.addChild("empty", new JsonValue(settings.rooms().empty()));
    rooms.addChild("collectible", new JsonValue(settings.rooms().collectible()));
    node.addChild("rooms", rooms);
    // Last, as before 0.11.0.
    node.addChild("questRoomRatio", new JsonValue(settings.questRoomRatio()));

    return node;
  }

  private static FloorSettings settings(JsonValue node) {
    if (node == null) {
      return null;
    }

    JsonValue loops = node.get("loops");
    // Documents from before 0.11.0 have no adaptive fields: they get the defaults of that time.
    JsonValue branches = node.get("branches");
    JsonValue rooms = node.get("rooms");
    RoomMix mix = rooms == null ? RoomMix.DEFAULT
        : new RoomMix(rooms.getDouble("shops"), rooms.getDouble("minibosses"),
            rooms.getDouble("normal"), rooms.getDouble("puzzle"), rooms.getDouble("empty"),
            rooms.getDouble("collectible"));

    return new FloorSettings(node.getInt("minRooms"), node.getInt("maxRooms"),
        node.getDouble("trunkRatio"), node.getDouble("criticalShare"),
        new LoopSettings(loops.getDouble("perRoom"), loops.getInt("minCycleRooms"),
            loops.getDouble("maxCriticalLoss")), node.getDouble("questRoomRatio"),
        node.getInt("questBundleMax", QuestSettings.defaults().maxBundleSize()),
        node.getDouble("linearity", 0),
        branches == null ? BranchSettings.NONE : new BranchSettings(
            branches.getDouble("chaining"), branches.getDouble("deadEndMinibosses")), mix);
  }

  private static FloorDocument.StateDoc state(JsonValue node) {
    if (node == null) {
      return FloorDocument.StateDoc.empty();
    }

    return new FloorDocument.StateDoc(strings(node.get("visited")), strings(node.get("cleared")),
        strings(node.get("opened")));
  }

  private static JsonValue child(JsonValue root, String name) {
    JsonValue node = root.get(name);

    return node == null ? null : node.child;
  }

  private static JsonValue object() {
    return new JsonValue(JsonValue.ValueType.object);
  }

  private static JsonValue array() {
    return new JsonValue(JsonValue.ValueType.array);
  }

  private static JsonValue strings(List<String> values) {
    JsonValue node = array();

    values.forEach(value -> node.addChild(new JsonValue(value)));

    return node;
  }

  private static List<String> strings(JsonValue node) {
    List<String> values = new ArrayList<>();

    for (JsonValue child = node == null ? null : node.child; child != null; child = child.next) {
      values.add(child.asString());
    }

    return values;
  }
}
