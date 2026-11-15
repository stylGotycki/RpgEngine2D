package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.Vector2;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.level.events.InteractEvent;

public class HatchScript extends EntityScript implements GameEventListener {

  // Half the side of the hatch (2 x 2 tiles): standing anywhere on it counts.
  public static final float HALF_SIZE = 1f;

  private final int player;

  private final float centerX;

  private final float centerY;

  private final boolean lastFloor;

  private final Runnable descend;

  private boolean inside;

  private boolean used;

  public HatchScript(int player, float centerX, float centerY, boolean lastFloor,
      Runnable descend) {
    this.player = player;
    this.centerX = centerX;
    this.centerY = centerY;
    this.lastFloor = lastFloor;
    this.descend = descend;
  }

  public static boolean covers(float centerX, float centerY, float x, float y) {
    return Math.abs(x - centerX) <= HALF_SIZE && Math.abs(y - centerY) <= HALF_SIZE;
  }

  @Override
  public void update(float delta) {
    if (used) {
      return;
    }

    boolean now = playerOnHatch();

    if (now && !inside) {
      Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = lastFloor
          ? "[E] Wyjdź z lochów" : "[E] Zejdź na następne piętro");
    }
    inside = now;
  }

  @Override
  public void onEvent(GameEvent event) {
    // The event bus outlives scenes; a hatch of an earlier floor must not react to this one.
    if (!(event instanceof InteractEvent) || used || getScene() == null
        || getEngine().getActiveScene() != getScene() || !playerOnHatch()) {
      return;
    }

    used = true;
    Engine.eventBus.unsubscribe(this, InteractEvent.class);
    descend.run();
  }

  private boolean playerOnHatch() {
    TransformComponent transform = getScene().getComponentStorage(TransformComponent.class)
        .getByEntity(player);

    if (transform == null) {
      return false;
    }

    Vector2 at = transform.position;
    return covers(centerX, centerY, at.x, at.y);
  }
}
