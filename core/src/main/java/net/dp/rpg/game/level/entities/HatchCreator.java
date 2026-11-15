package net.dp.rpg.game.level.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.BodyDef;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.level.events.InteractEvent;
import net.dp.rpg.game.scripts.HatchScript;

// Builds the hatch: a floor sprite (underlay, so the player walks over it) and the script that
// leads down. Creates bodies, so it must run outside the physics step.
public class HatchCreator {

  public static final String REGION = "level/hatch";

  private static final float SIZE = 2f;

  private final Scene scene;

  private final TextureAtlas atlas;

  public HatchCreator(Scene scene, TextureAtlas atlas) {
    this.scene = scene;
    this.atlas = atlas;
  }

  public HatchScript create(float centerX, float centerY, int player, boolean lastFloor,
      Runnable descend) {
    Sprite sprite = atlas.createSprite(REGION);

    if (sprite == null) {
      throw new IllegalStateException("No region " + REGION + " in the atlas; repack textures");
    }

    sprite.setSize(SIZE, SIZE);
    sprite.setOriginCenter();
    sprite.setCenter(centerX, centerY);

    // Like a puzzle tile: a static body without a fixture gives the sprite a transform to follow.
    BodyDef body = new BodyDef();
    body.type = BodyDef.BodyType.StaticBody;
    body.position.set(centerX, centerY);
    int visual = scene.createEntity();
    scene.createBodyComponent(visual, body);
    scene.addComponent(visual, new SpriteComponent(sprite, true, true));

    HatchScript script = new HatchScript(player, centerX, centerY, lastFloor, descend);
    int logic = scene.createEntity();
    scene.addComponent(logic, new ScriptComponent(script));
    Engine.eventBus.subscribe(script, InteractEvent.class);

    return script;
  }
}
