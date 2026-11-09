package net.dp.rpg.game.level.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Disposable;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.item.ItemDef;
import net.dp.rpg.game.scripts.ItemPickupScript;

public class ItemPickupCreator implements Disposable {

  private static final float SIZE = 0.7f;

  private final Scene scene;

  private final TextureAtlas atlas;

  private final BodyDef bodyDef = new BodyDef();

  private final FixtureDef fixtureDef = new FixtureDef();

  private final CircleShape circle = new CircleShape();

  public ItemPickupCreator(Scene scene, TextureAtlas atlas) {
    this.scene = scene;
    this.atlas = atlas;

    bodyDef.type = BodyDef.BodyType.StaticBody;
    circle.setRadius(SIZE / 2);
    fixtureDef.shape = circle;
    fixtureDef.isSensor = true;
    fixtureDef.filter.categoryBits = FixtureCategory.ITEM;
  }

  public int create(ItemDef item, float x, float y, String group) {
    bodyDef.position.set(x, y);

    int entity = scene.createEntity();
    scene.createBodyComponent(entity, bodyDef);
    scene.createFixture(entity, fixtureDef);

    Sprite sprite = atlas.createSprite(item.icon());
    if (sprite == null) {
      throw new IllegalStateException("No icon " + item.icon() + " in the atlas; repack textures");
    }
    sprite.setSize(SIZE, SIZE);
    sprite.setOriginCenter();
    sprite.setCenter(x, y);
    scene.addComponent(entity, new SpriteComponent(sprite, true));
    scene.addComponent(entity, new ScriptComponent(new ItemPickupScript(item, group)));

    return entity;
  }

  @Override
  public void dispose() {
    circle.dispose();
  }
}
