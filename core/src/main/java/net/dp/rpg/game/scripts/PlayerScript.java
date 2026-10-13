package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import net.dp.rpg.engine.*;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.level.EnterRoomEvent;
import net.dp.rpg.game.level.HealthChangeEvent;
import net.dp.rpg.game.menu.MenuBuilder;

public class PlayerScript extends EntityScript implements InputListener
{
    private final Fixture playerFixture;
    private final Fixture attackFixture;
    private final Fixture sensorFixture;

    private final Vector2 tempVector2 = new Vector2();
    private final Vector2 attackOffset = new Vector2();
    private final float attackFadeTime = 0.2f;
    private float remainingAttackFade = 0;
    boolean isAttacking = false;
    TileRoom roomEntered = null;
    TileRoom currentRoom = null;
    DoorScript doorReached = null;

    public PlayerScript(Fixture playerFixture, Fixture attackFixture, Fixture sensorFixture)
    {
        this.playerFixture = playerFixture;
        this.attackFixture = attackFixture;
        this.sensorFixture = sensorFixture;
    }

    @Override
    public void update(float delta)
    {
        float speed = 60f;
        tempVector2.set(0,0);
        if(Gdx.input.isKeyPressed(Input.Keys.A))
        {
            tempVector2.x -= 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D))
        {
            tempVector2.x += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W))
        {
            tempVector2.y += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S))
        {
            tempVector2.y -= 1;
        }

        tempVector2.setLength(1);

        Body playerBody = getComponent(PhysicalBodyComponent.class).body;
        playerBody.applyForce(tempVector2.scl(speed), playerBody.getPosition(), true);

        if(remainingAttackFade > 0)
        {
            remainingAttackFade -= delta;
            if(remainingAttackFade < 0)
            {
                remainingAttackFade = 0;
                stopAttack();
            }
            getComponent(SpriteComponent.class).sprites.get(1).setAlpha(remainingAttackFade/attackFadeTime);
        }

        if(roomEntered != null)
        {
            EnterRoomEvent event = getEngine().getPoolManager().obtain(EnterRoomEvent.class);
            event.room = roomEntered;
            getEngine().getEventBus().publish(event);
            getEngine().getPoolManager().free(event);
            currentRoom = roomEntered;
            roomEntered = null;
        }
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if((otherCategory & FixtureCategory.ENEMY) > 0 && (category & FixtureCategory.PLAYER) > 0)
        {
            HealthComponent health = getComponent(HealthComponent.class);
            health.dealDamage(20);
            HealthChangeEvent event = getEngine().getPoolManager().obtain(HealthChangeEvent.class);
            event.currentHealth = health.health;
            getEngine().getEventBus().publish(event);
            getEngine().getPoolManager().free(event);
            Body body = getComponent(PhysicalBodyComponent.class).body;
            Vector2 otherPosition = getComponent(TransformComponent.class).position;
            Vector2 push = tempVector2.set(body.getPosition()).sub(otherPosition).scl(24);
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
        if((otherCategory & FixtureCategory.OBSTACLE) > 0 && (category & FixtureCategory.PLAYER) > 0)
        {
            Script script = getScene().getComponentStorage(ScriptComponent.class).getByEntity(otherEntity).script;
            if(script instanceof DoorScript doorScript)
            {
                if(currentRoom == null)
                    currentRoom = doorScript.getRoom();
                if(doorScript.getRoom() != currentRoom)
                {
                    roomEntered = doorScript.getRoom();
                }
                else
                    return;
                TransformComponent transformComponent = getComponent(TransformComponent.class);
                Direction doorDirection = doorScript.getRoomEdge().opposite();
                if(doorDirection == Direction.NORTH || doorDirection == Direction.SOUTH)
                    doorDirection = doorDirection.opposite();
                transformComponent.setPosition( transformComponent.position.add(Utils.directionToVector2(doorDirection, tempVector2)));
            }
        }
        if((otherCategory & FixtureCategory.OBSTACLE) > 0 && (category & FixtureCategory.SENSOR) > 0)
        {
            System.out.println("Enter");
            Script script = getScene().getComponentStorage(ScriptComponent.class).getByEntity(otherEntity).script;
            if(script instanceof DoorScript doorScript)
            {
                doorReached = doorScript;
            }
        }
    }

    @Override
    public void onEndCollision(int otherEntity, short category, short otherCategory)
    {
        if((otherCategory & FixtureCategory.OBSTACLE) > 0 && (category & FixtureCategory.SENSOR) > 0)
        {
            System.out.println("Exit");
            Script script = getScene().getComponentStorage(ScriptComponent.class).getByEntity(otherEntity).script;
            if(script instanceof DoorScript doorScript)
            {
                if(doorScript == doorReached)
                    doorReached = null;
            }
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
        else if(keycode == Input.Keys.E)
        {
            if(doorReached != null)
                doorReached.open();
        }
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

        PolygonShape shape = (PolygonShape) attackFixture.getShape();
        shape.setAsBox(Math.abs(direction.getDeltaY())*0.3f + 0.3f, Math.abs(direction.getDeltaX())*0.3f + 0.3f, attackOffset, 0);
        attackFixture.getFilterData().maskBits = (short)0xFFFF;
        isAttacking = true;
        remainingAttackFade = attackFadeTime;
        Sprite sprite = getComponent(SpriteComponent.class).sprites.get(1);
        sprite.setAlpha(1);
        sprite.setOriginCenter();
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
        attackFixture.getFilterData().maskBits = 0;
        isAttacking = false;
    }
}
