package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.Input;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.input.InputListener;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.level.FloorTransition;
import net.dp.rpg.game.level.events.PlayerDeathEvent;
import net.dp.rpg.game.scripts.LevelScript;

// F3 shows the adaptation overlay; F5 leaves the floor like the hatch does, even with the boss
// alive (a debug shortcut). Extends the level script from dev, which takes the player back to the
// menu after death; here the dying floor is also written to the log and stops being measured.
public class AdaptationScript extends LevelScript implements InputListener {

  private final GameSession session;

  private final PlayerStats stats;

  private final AdaptationOverlay overlay;

  private final int player;

  public AdaptationScript(GameSession session, PlayerStats stats, AdaptationOverlay overlay,
      int player) {
    this.session = session;
    this.stats = stats;
    this.overlay = overlay;
    this.player = player;
  }

  @Override
  public void update(float delta) {
    super.update(delta);
  }

  @Override
  public void onEvent(GameEvent event) {
    super.onEvent(event);

    // Death ends the game, and the next game starts a new session, i.e. the neutral profile.
    if (event instanceof PlayerDeathEvent) {
      session.abandonFloor();
    }
  }

  @Override
  public boolean keyDown(int keyCode) {
    if (keyCode == Input.Keys.F3) {
      overlay.toggle();
      return true;
    }

    if (keyCode == Input.Keys.F5) {
      FloorTransition.descend(getEngine(), getScene(), session, stats, player);
      return true;
    }

    return false;
  }

  @Override
  public boolean keyUp(int keyCode) {
    return false;
  }
}
