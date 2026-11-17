package net.dp.rpg.game.level.entities;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Disposable;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.scripts.*;

public class SpellCreator implements Disposable
{
    private final Scene scene;

    private final TextureAtlas atlas;

    private final BodyDef bodyDef = new BodyDef();

    private final FixtureDef fixtureDef = new FixtureDef();

    private final CircleShape circle = new CircleShape();

    private final float size = 0.3f;

    public SpellCreator(Scene scene, TextureAtlas atlas)
    {
        this.scene = scene;
        this.atlas = atlas;

        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;

        circle.setRadius(size/2);

        fixtureDef.restitution = 0f;
        fixtureDef.friction = 0f;
        fixtureDef.density = 0f;
        fixtureDef.filter.categoryBits = FixtureCategory.PLAYER_ATTACK;
        fixtureDef.isSensor = true;
        fixtureDef.shape = circle;
    }

    public int create(float x, float y, float damage, float knockback, float speed, Vector2 direction)
    {
        bodyDef.position.set(x, y);

        Sprite sprite = atlas.createSprite("spell");
        sprite.setSize(size, size);
        sprite.setOriginCenter();

        int entityId = scene.createEntity();
        scene.createBodyComponent(entityId, bodyDef);
        scene.createFixture(entityId, fixtureDef);
        scene.addComponent(entityId, new SpriteComponent(sprite, true));
        scene.addComponent(entityId, new AttackComponent(damage, knockback));
        scene.addComponent(entityId, new FractionComponent(Fraction.PLAYER));
        scene.addComponent(entityId, new ScriptComponent(new SpellScript(speed, direction)));

        return entityId;
    }

    @Override
    public void dispose()
    {
        circle.dispose();
    }
}
