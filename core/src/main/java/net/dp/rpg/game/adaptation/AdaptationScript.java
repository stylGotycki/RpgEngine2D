package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.Input;
import java.util.Random;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.SceneScript;
import net.dp.rpg.engine.systems.input.InputListener;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.level.LevelBuilder;

// F3 shows the adaptation overlay; F5 ends the floor and builds the next one from the updated
// profile. F5 stands in for the stairs after the boss until they exist.
public class AdaptationScript extends SceneScript implements InputListener {

  private final GameSession session;

  private final PlayerStats stats;

  private final AdaptationOverlay overlay;

  public AdaptationScript(GameSession session, PlayerStats stats, AdaptationOverlay overlay) {
    this.session = session;
    this.stats = stats;
    this.overlay = overlay;
  }

  @Override
  public void update(float delta) {
  }

  @Override
  public boolean keyDown(int keyCode) {
    if (keyCode == Input.Keys.F3) {
      overlay.toggle();
      return true;
    }

    if (keyCode == Input.Keys.F5) {
      session.finishFloor();
      Scene scene = getEngine().createScene();
      new LevelBuilder(new Random().nextLong(), stats, session).build(getEngine(), scene);
      getEngine().switchScene(scene);
      return true;
    }

    return false;
  }

  @Override
  public boolean keyUp(int keyCode) {
    return false;
  }
}
