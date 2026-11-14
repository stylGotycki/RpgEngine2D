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
import net.dp.rpg.game.animations.SpiderAnimationId;
import net.dp.rpg.game.scripts.SpiderScript;

public class SpiderEntityCreator extends EntityCreator implements Disposable
{
    private final float size = 0.5f;
    private final int health = 50;
    private final float damage = 1;
    private final float knockback = 10;
    private final float damageDelay = 1;
    private final float playerDetectionSensorSize = size * 12;

    private final BodyDef bodyDef = new BodyDef();
    private final FixtureDef fixtureDef = new FixtureDef();
    private final FixtureDef sensorFixtureDef = new FixtureDef();
    private final CircleShape circle = new CircleShape();
    private final CircleShape sensorCircle = new CircleShape();
    private final Sprite sprite = new Sprite();
    private final Animation<TextureRegion> walkAnimation;

    private final TextureAtlas atlas;

    public SpiderEntityCreator(Scene scene, TextureAtlas atlas)
    {
        super(scene);
        this.atlas = atlas;

        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = DefaultEntityProperties.LINEAR_DAMPING;
        bodyDef.angularDamping = DefaultEntityProperties.ANGULAR_DAMPING;

        circle.setRadius(size/2);
        sensorCircle.setRadius(playerDetectionSensorSize);

        fixtureDef.restitution = 0f;
        fixtureDef.friction = 0f;
        fixtureDef.density = 1f;
        fixtureDef.filter.categoryBits = (short)(FixtureCategory.ENEMY | FixtureCategory.ENEMY_ATTACK);
        fixtureDef.isSensor = false;
        fixtureDef.shape = circle;

        sensorFixtureDef.density = 0f;
        sensorFixtureDef.filter.categoryBits = (short)(FixtureCategory.SENSOR);
        sensorFixtureDef.isSensor = true;
        sensorFixtureDef.shape = sensorCircle;

        float spriteSize = size * 1.6f;
        sprite.setSize(spriteSize, spriteSize);
        sprite.setOriginCenter();

        walkAnimation = new Animation<>(0.1f, atlas.findRegions("enemy/walk"), Animation.PlayMode.LOOP);
    }

    @Override
    public int create(float x, float y)
    {
        bodyDef.position.set(x,y);

        AnimationSet<SpiderAnimationId> animationSet = new AnimationSet<>();
        animationSet.addAnimation(walkAnimation, SpiderAnimationId.WALK);

        int entityId = scene.createEntity();
        scene.createBodyComponent(entityId, bodyDef);
        scene.createFixture(entityId, fixtureDef);
        scene.createFixture(entityId, sensorFixtureDef);
        scene.addComponent(entityId, new HealthComponent(health,health));
        scene.addComponent(entityId, new PeriodicAttackComponent(damage, knockback, damageDelay));
        scene.addComponent(entityId, new FractionComponent(Fraction.ENEMY));
        scene.addComponent(entityId, new SpriteComponent(new Sprite(sprite), false));
        scene.addComponent(entityId, new AnimationComponent(animationSet));
        scene.addComponent(entityId, new ScriptComponent(new SpiderScript()));

        return entityId;
    }

    @Override
    public void dispose()
    {
        circle.dispose();
        sensorCircle.dispose();
    }
}
