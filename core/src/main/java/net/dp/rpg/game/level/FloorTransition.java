package net.dp.rpg.game.level;

import com.badlogic.gdx.Gdx;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.game.adaptation.GameSession;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.victory.VictoryBuilder;

// Leaving a floor: the hatch after the boss and the debug F5 both end up here. The floor is
// finished (metrics -> profile -> log), then either the next floor is built from the updated
// profile or, after the last one, the victory screen opens.
public final class FloorTransition {

  private FloorTransition() {
  }

  public static void descend(EngineServices engine, Scene scene, GameSession session,
      PlayerStats stats, int player) {
    // Always on the next frame: this is called from a key handler or a physics callback, and the
    // old scene is disposed by the switch, which must not happen in the middle of either.
    Gdx.app.postRunnable(() -> {
      // A second press before the frame ran, or a scene already left, does nothing.
      if (engine.getActiveScene() != scene) {
        return;
      }

      PlayerCarry carry = PlayerCarry.capture(scene, player);
      session.finishFloor();

      Scene next = engine.createScene();
      if (session.adaptive().finished()) {
        new VictoryBuilder().build(engine, next, session);
      } else {
        new LevelBuilder(session.nextSeed(), stats, session, carry).build(engine, next);
      }
      engine.switchScene(next);
    });
  }
}
