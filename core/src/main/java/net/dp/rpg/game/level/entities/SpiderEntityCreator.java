package net.dp.rpg.game.level.entities;

import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Disposable;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.animations.EnemyAnimationId;
import net.dp.rpg.game.scripts.EnemyScript;

public class SpiderEntityCreator extends EntityCreator implements Disposable
{
    private final float size = 0.5f;
    private final int health = 50;

    private final BodyDef bodyDef = new BodyDef();
    private final FixtureDef fixtureDef = new FixtureDef();
    private final CircleShape circle = new CircleShape();
    private final Sprite sprite = new Sprite();
    private final Animation<TextureRegion> walkAnimation;

    private final TextureAtlas atlas;
    private final int playerId;

    public SpiderEntityCreator(Scene scene, TextureAtlas atlas, int playerId)
    {
        super(scene);
        this.atlas = atlas;
        this.playerId = playerId;

        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = DefaultEntityProperties.LINEAR_DAMPING;
        bodyDef.angularDamping = DefaultEntityProperties.ANGULAR_DAMPING;

        circle.setRadius(size/2);

        fixtureDef.restitution = 0f;
        fixtureDef.friction = 0f;
        fixtureDef.density = 1f;
        fixtureDef.filter.categoryBits = FixtureCategory.ENEMY;
        fixtureDef.isSensor = false;
        fixtureDef.shape = circle;

        float spriteSize = size * 1.6f;
        sprite.setSize(spriteSize, spriteSize);
        sprite.setOriginCenter();

        walkAnimation = new Animation<>(0.1f, atlas.findRegions("enemy/walk"), Animation.PlayMode.LOOP);
    }

    @Override
    public int create(float x, float y)
    {
        bodyDef.position.set(x,y);

        AnimationSet<EnemyAnimationId> animationSet = new AnimationSet<>();
        animationSet.addAnimation(walkAnimation, EnemyAnimationId.WALK);

        int entityId = scene.createEntity();
        scene.createBodyComponent(entityId, bodyDef);
        scene.createFixture(entityId, fixtureDef);
        scene.addComponent(entityId, new HealthComponent(health,health));
        scene.addComponent(entityId, new SpriteComponent(new Sprite(sprite), false));
        scene.addComponent(entityId, new AnimationComponent(animationSet));
        scene.addComponent(entityId, new ScriptComponent(new EnemyScript(playerId)));

        return entityId;
    }

    @Override
    public void dispose()
    {
        circle.dispose();
    }
}
