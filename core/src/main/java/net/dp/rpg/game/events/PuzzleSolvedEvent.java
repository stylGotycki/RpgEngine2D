package net.dp.rpg.game.events;

import net.dp.rpg.engine.puzzle.PuzzleKind;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.TileRoom;

public class PuzzleSolvedEvent implements GameEvent {

  public PuzzleKind kind;

  public String questId;

  public TileRoom room;

  @Override
  public void reset() {
    kind = null;
    questId = null;
    room = null;
  }
}
