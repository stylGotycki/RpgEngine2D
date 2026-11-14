package net.dp.rpg.game.level.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Disposable;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.MultiSpriteComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.scripts.NpcScript;

public class NpcCreator implements Disposable {

  private static final float SIZE = 0.8f;

  private final Scene scene;

  private final TextureAtlas atlas;

  private final BodyDef bodyDef = new BodyDef();

  private final FixtureDef fixtureDef = new FixtureDef();

  private final CircleShape circle = new CircleShape();

  public NpcCreator(Scene scene, TextureAtlas atlas) {
    this.scene = scene;
    this.atlas = atlas;

    bodyDef.type = BodyDef.BodyType.StaticBody;
    circle.setRadius(SIZE / 2);
    fixtureDef.shape = circle;
    fixtureDef.filter.categoryBits = FixtureCategory.NPC;
  }

  public int create(String sprite, float x, float y, NpcScript script) {
    bodyDef.position.set(x, y);

    int entity = scene.createEntity();
    scene.createBodyComponent(entity, bodyDef);
    scene.createFixture(entity, fixtureDef);

    Sprite body = atlas.createSprite(sprite);
    if (body == null) {
      throw new IllegalStateException("No sprite " + sprite + " in the atlas");
    }
    body.setSize(SIZE * 1.3f, SIZE * 1.3f);
    body.setOriginCenter();
    body.setOriginBasedPosition(x, y);

    Sprite marker = atlas.createSprite("npc/exclaim");
    marker.setSize(0.25f, 0.5f);
    marker.setOrigin(0.125f, -0.55f);
    marker.setOriginBasedPosition(x, y);

    MultiSpriteComponent sprites = new MultiSpriteComponent();
    sprites.add(body, true);
    sprites.add(marker, true);
    scene.addComponent(entity, sprites);
    script.setMarker(marker);
    scene.addComponent(entity, new ScriptComponent(script));

    return entity;
  }

  @Override
  public void dispose() {
    circle.dispose();
  }
}
