package net.dp.rpg.game.level.entities.player;

import box2dLight.Light;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Disposable;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.animations.PlayerAnimationId;
import net.dp.rpg.game.level.entities.DefaultEntityProperties;
import net.dp.rpg.game.level.entities.EntityCreator;
import net.dp.rpg.game.level.entities.Fraction;
import net.dp.rpg.game.scripts.PlayerScript;

public class PlayerCreator extends EntityCreator implements Disposable
{
    private final float size = 0.8f;
    private final int health = 100;
    private final float damage = 20;
    private final float knockback = 8;
    private final float afterDamageInvincibility = 0.5f;

    private final BodyDef bodyDef = new BodyDef();
    private final FixtureDef bodyFixtureDef = new FixtureDef();
    private final FixtureDef attackFixtureDef = new FixtureDef();
    private final FixtureDef sensorFixtureDef = new FixtureDef();
    private final CircleShape bodyCircle = new CircleShape();
    private final CircleShape sensorCircle = new CircleShape();
    private final PolygonShape attackBox = new PolygonShape();
    private final Sprite bodySprite = new Sprite();
    private final Sprite attackSprite;
    private final Animation<TextureRegion> idleAnimation;
    private final Animation<TextureRegion> walkAnimation;
    private final Animation<TextureRegion> deathAnimation;

    private final TextureAtlas atlas;
    private final PlayerStats stats;
    private final DialogueController dialogues;

    public PlayerCreator(Scene scene, TextureAtlas atlas, PlayerStats stats, DialogueController dialogues)
    {
        super(scene);

        this.atlas = atlas;
        this.stats = stats;
        this.dialogues = dialogues;

        //player Box2D body
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = DefaultEntityProperties.LINEAR_DAMPING;
        bodyDef.angularDamping = DefaultEntityProperties.ANGULAR_DAMPING;

        //body fixture
        bodyCircle.setRadius(size/2);

        bodyFixtureDef.restitution = 0f;
        bodyFixtureDef.friction = 0f;
        bodyFixtureDef.density = 1f;
        bodyFixtureDef.filter.categoryBits = FixtureCategory.PLAYER;
        bodyFixtureDef.isSensor = false;
        bodyFixtureDef.shape = bodyCircle;

        //attack fixture
        attackBox.setAsBox(size, size);

        attackFixtureDef.density = 0f;
        attackFixtureDef.isSensor = true;
        attackFixtureDef.filter.categoryBits = FixtureCategory.PLAYER_ATTACK;
        attackFixtureDef.shape = attackBox;

        //sensor fixture
        sensorCircle.setRadius(size * 1.5f);

        sensorFixtureDef.density = 0f;
        sensorFixtureDef.isSensor = true;
        sensorFixtureDef.filter.categoryBits = FixtureCategory.SENSOR;
        sensorFixtureDef.shape = sensorCircle;

        //sprites
        float spriteSize = size * 1.25f;
        bodySprite.setSize(spriteSize, spriteSize);
        bodySprite.setOriginCenter();

        attackSprite = atlas.createSprite("attack");
        attackSprite.setSize(1.75f, 1.75f/2);
        attackSprite.setOrigin(attackSprite.getOriginX(), attackSprite.getOriginY()-0.8f);
        attackSprite.setAlpha(0);

        idleAnimation = new Animation<>(1f, atlas.findRegions("player/idle"), Animation.PlayMode.LOOP);
        walkAnimation = new Animation<>(0.1f, atlas.findRegions("player/walk"), Animation.PlayMode.LOOP);
        deathAnimation = new Animation<>(0.1f, atlas.findRegions("player/death"), Animation.PlayMode.NORMAL);

    }

    @Override
    public int create(float x, float y)
    {
        bodyDef.position.set(x,y);

        AnimationSet<PlayerAnimationId> animationSet = new AnimationSet<>();
        animationSet.addAnimation(idleAnimation, PlayerAnimationId.IDLE);
        animationSet.addAnimation(walkAnimation, PlayerAnimationId.WALK);
        animationSet.addAnimation(deathAnimation, PlayerAnimationId.DEATH);

        MultiSpriteComponent spriteComponent = new MultiSpriteComponent();
        spriteComponent.add(bodySprite, true);
        spriteComponent.add(attackSprite, true);

        int entityId = scene.createEntity();

        scene.createBodyComponent(entityId, bodyDef);
        Fixture bodyFixture = scene.createFixture(entityId, bodyFixtureDef);
        Fixture sensorFixture = scene.createFixture(entityId, sensorFixtureDef);

        Filter lightContactFilter = new Filter();
        lightContactFilter.categoryBits = FixtureCategory.LIGHT;
        lightContactFilter.maskBits = (short)(FixtureCategory.OBSTACLE | FixtureCategory.WALL);
        Light light = scene.createPointLightComponent(entityId, 128, new Color(1f, 0.6f, 0.1f, 0.8f), size * 32, false);
        light.setContactFilter(lightContactFilter);

        scene.addComponent(entityId, new HealthComponent(health,health, afterDamageInvincibility));
        scene.addComponent(entityId, new AttackComponent(damage,knockback));
        scene.addComponent(entityId, new FractionComponent(Fraction.PLAYER));
        scene.addComponent(entityId, spriteComponent);

        MultiAnimationComponent animationComponent = new MultiAnimationComponent();
        animationComponent.add(animationSet);

        scene.addComponent(entityId, animationComponent);

        scene.addComponent(entityId, new ScriptComponent(new PlayerScript(bodyFixture, attackFixtureDef, sensorFixture, stats, dialogues)));

        return entityId;
    }

    @Override
    public void dispose()
    {
        bodyCircle.dispose();
    }
}
