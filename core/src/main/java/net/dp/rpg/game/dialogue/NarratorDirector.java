package net.dp.rpg.game.dialogue;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import net.dp.rpg.engine.dialogue.DialogueLibrary;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.level.events.EnterRoomEvent;

public class NarratorDirector implements GameEventListener {

  public static final String FLOOR_START = "narrator.floor-start";

  public static final String ROOM_PREFIX = "narrator.room.";

  private final TileFloor floor;

  private final DialogueController dialogues;

  private final DialogueLibrary library;

  private final Map<String, String> variables;

  private final Set<RoomType> told = new HashSet<>();

  public NarratorDirector(TileFloor floor, DialogueController dialogues, DialogueLibrary library,
      Map<String, String> variables) {
    this.floor = floor;
    this.dialogues = dialogues;
    this.library = library;
    this.variables = Map.copyOf(variables);
  }

  public void floorStart() {
    tell(FLOOR_START);
  }

  // Each special room is introduced once per floor, the first time the player walks in.
  @Override
  public void onEvent(GameEvent event) {
    if (event instanceof EnterRoomEvent enter && floor.owns(enter.room)) {
      RoomType type = enter.room.node().type();

      if (told.add(type)) {
        tell(ROOM_PREFIX + type.name());
      }
    }
  }

  private void tell(String id) {
    if (library.has(id) && !dialogues.isOpen()) {
      dialogues.open(library.get(id), variables, action -> {
      });
    }
  }
}
