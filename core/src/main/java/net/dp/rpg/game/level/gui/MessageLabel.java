package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.events.GameMessageEvent;

public class MessageLabel extends Label implements GameEventListener {

  private static final float SHOWN = 2.5f;

  private static final float FADE = 0.5f;

  private float remaining;

  public MessageLabel(Skin skin) {
    super("", skin, GameFonts.LABEL);
    getColor().a = 0;
  }

  @Override
  public void onEvent(GameEvent event) {
    if (event instanceof GameMessageEvent message) {
      setText(message.text);
      remaining = SHOWN;
    }
  }

  @Override
  public void act(float delta) {
    super.act(delta);

    if (remaining > 0) {
      remaining -= delta;
    }
    getColor().a = Math.max(0, Math.min(1, remaining / FADE));
  }
}
