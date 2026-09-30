package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.InputListener;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.menu.MenuBuilder;

public class PlayerScript extends EntityScript implements InputListener
{
    private final Vector2 direction = new Vector2();
    private final Vector2 attackOffset = new Vector2();
    private final float attackFadeTime = 0.2f;
    private float remainingAttackFade = 0;
    boolean isAttacking = false;

    @Override
    public void update(float delta)
    {
        float speed = 60f;
        direction.set(0,0);
        if(Gdx.input.isKeyPressed(Input.Keys.A))
        {
            direction.x -= 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D))
        {
            direction.x += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W))
        {
            direction.y += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S))
        {
            direction.y -= 1;
        }

        direction.setLength(1);

        Body etiBody = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(getEntity()).body;
        etiBody.applyForce(direction.scl(speed), etiBody.getPosition(), true);

        if(remainingAttackFade > 0)
        {
            remainingAttackFade -= delta;
            if(remainingAttackFade < 0)
            {
                remainingAttackFade = 0;
                stopAttack();
            }
            getScene().getComponentStorage(SpriteComponent.class).getByEntity(getEntity()).sprites.get(1).setAlpha(remainingAttackFade/attackFadeTime);
        }

    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if((otherCategory & FixtureCategory.ENEMY) > 0 && (category & FixtureCategory.PLAYER) > 0)
        {
            HealthComponent health = getScene().getComponentStorage(HealthComponent.class).getByEntity(getEntity());
            health.dealDamage(20);
            Body body = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(getEntity()).body;
            Vector2 otherPosition = getScene().getComponentStorage(TransformComponent.class).getByEntity(otherEntity).position;
            Vector2 push = direction.set(body.getPosition()).sub(otherPosition).scl(24);
            body.applyLinearImpulse(push, body.getPosition(), true);
            if(health.health <= 0)
            {
                MenuBuilder menuBuilder = new MenuBuilder();
                Scene scene = getEngine().createScene();
                menuBuilder.build(getEngine(), scene);
                getEngine().switchScene(scene);
            }
        }
        if((otherCategory & FixtureCategory.ENEMY) > 0 && (category & FixtureCategory.PLAYER_ATTACK) > 0)
        {
            stopAttack();
        }
    }

    @Override
    public boolean keyDown(int keycode)
    {
        if(keycode == Input.Keys.UP)
            attack(Direction.NORTH);
        else if(keycode == Input.Keys.RIGHT)
            attack(Direction.EAST);
        else if(keycode == Input.Keys.DOWN)
            attack(Direction.SOUTH);
        else if(keycode == Input.Keys.LEFT)
            attack(Direction.WEST);
        else
            return false;
        return true;
    }

    @Override
    public boolean keyUp(int keycode)
    {
        return false;
    }

    private void attack(Direction direction)
    {
        if(isAttacking)
            return;
        attackOffset.set(direction.getDeltaX(), -1 * direction.getDeltaY());
        Fixture fixture = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(getEntity()).body.getFixtureList().get(1);
        PolygonShape shape = (PolygonShape) fixture.getShape();
        shape.setAsBox(Math.abs(direction.getDeltaY())*0.3f + 0.3f, Math.abs(direction.getDeltaX())*0.3f + 0.3f, attackOffset, 0);
        fixture.getFilterData().maskBits = (short)0xFFFF;
        isAttacking = true;
        remainingAttackFade = attackFadeTime;
        Sprite sprite = getScene().getComponentStorage(SpriteComponent.class).getByEntity(getEntity()).sprites.get(1);
        sprite.setAlpha(1);
        sprite.setOriginCenter();
        //sprite.setOrigin(sprite.getOriginX() + attackOffset.x*-1, sprite.getOriginY() + attackOffset.y*-1);
        sprite.setOrigin(sprite.getOriginX(), sprite.getOriginY() - 1);
        switch(direction)
        {
            case NORTH -> sprite.setRotation(0);
            case EAST -> sprite.setRotation(270);
            case SOUTH -> sprite.setRotation(180);
            case WEST -> sprite.setRotation(90);
        }
    }

    private void stopAttack()
    {
        Fixture fixture = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(getEntity()).body.getFixtureList().get(1);
        fixture.getFilterData().maskBits = 0;
        isAttacking = false;
    }
}
