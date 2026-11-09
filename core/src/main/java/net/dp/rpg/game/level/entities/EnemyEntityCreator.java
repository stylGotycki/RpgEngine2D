package net.dp.rpg.game.level.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.Disposable;
import java.util.HashMap;
import java.util.Map;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.AnimationComponent;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.animations.EnemyAnimationId;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.scripts.EnemyScript;

public class EnemyEntityCreator implements Disposable {

  private final Scene scene;

  private final TextureAtlas atlas;

  private final int playerId;

  private final DialogueController dialogues;

  private final BodyDef bodyDef = new BodyDef();

  private final FixtureDef fixtureDef = new FixtureDef();

  private final CircleShape circle = new CircleShape();

  private final Map<String, Animation<TextureRegion>> animations = new HashMap<>();

  public EnemyEntityCreator(Scene scene, TextureAtlas atlas, int playerId,
      DialogueController dialogues) {
    this.dialogues = dialogues;
    this.scene = scene;
    this.atlas = atlas;
    this.playerId = playerId;

    bodyDef.type = BodyDef.BodyType.DynamicBody;
    bodyDef.fixedRotation = true;
    bodyDef.linearDamping = DefaultEntityProperties.LINEAR_DAMPING;
    bodyDef.angularDamping = DefaultEntityProperties.ANGULAR_DAMPING;

    fixtureDef.restitution = 0f;
    fixtureDef.friction = 0f;
    fixtureDef.density = 1f;
    fixtureDef.filter.categoryBits = FixtureCategory.ENEMY;
    fixtureDef.isSensor = false;
    fixtureDef.shape = circle;
  }

  public int create(EnemyDef enemy, float x, float y, TileFloor floor, TileRoom home) {
    bodyDef.position.set(x, y);
    circle.setRadius(enemy.size() / 2);

    AnimationSet<EnemyAnimationId> animationSet = new AnimationSet<>();
    animationSet.addAnimation(animation(enemy), EnemyAnimationId.WALK);

    Sprite sprite = new Sprite();
    float spriteSize = enemy.size() * enemy.spriteScale();
    sprite.setSize(spriteSize, spriteSize);
    sprite.setOriginCenter();

    int entityId = scene.createEntity();
    scene.createBodyComponent(entityId, bodyDef);
    scene.createFixture(entityId, fixtureDef);
    scene.addComponent(entityId, new HealthComponent(enemy.health(), enemy.health()));
    scene.addComponent(entityId, new SpriteComponent(sprite, false));
    scene.addComponent(entityId, new AnimationComponent(animationSet));
    scene.addComponent(entityId,
        new ScriptComponent(new EnemyScript(playerId, enemy, floor, home, dialogues)));

    return entityId;
  }

  private Animation<TextureRegion> animation(EnemyDef enemy) {
    return animations.computeIfAbsent(enemy.id(), id -> {
      Array<TextureAtlas.AtlasRegion> frames = atlas.findRegions(enemy.sprite());

      if (frames.isEmpty()) {
        throw new IllegalStateException("No frames " + enemy.sprite() + " in the atlas");
      }

      return new Animation<>(enemy.frameTime(), frames, Animation.PlayMode.LOOP);
    });
  }

  @Override
  public void dispose() {
    circle.dispose();
  }
}
