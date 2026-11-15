package net.dp.rpg.game.victory;

import com.badlogic.gdx.Input;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.SceneScript;
import net.dp.rpg.engine.systems.input.InputListener;
import net.dp.rpg.game.menu.MenuBuilder;

// Enter on the victory screen goes back to the menu.
public class VictoryScript extends SceneScript implements InputListener {

  @Override
  public void update(float delta) {
  }

  @Override
  public boolean keyDown(int keyCode) {
    if (keyCode != Input.Keys.ENTER && keyCode != Input.Keys.NUMPAD_ENTER) {
      return false;
    }

    Scene menu = getEngine().createScene();
    new MenuBuilder().build(getEngine(), menu);
    getEngine().switchScene(menu);
    return true;
  }

  @Override
  public boolean keyUp(int keyCode) {
    return false;
  }
}
