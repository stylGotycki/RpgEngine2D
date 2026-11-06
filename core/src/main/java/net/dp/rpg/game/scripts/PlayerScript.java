package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.utils.IntSet;
import net.dp.rpg.engine.*;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.systems.input.InputListener;
import net.dp.rpg.engine.tile.room.Direction;
import static net.dp.rpg.game.FixtureCategory.*;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.animations.PlayerAnimationId;
import net.dp.rpg.game.events.*;
import net.dp.rpg.game.menu.MenuBuilder;

public class PlayerScript extends EntityScript implements InputListener, GameEventListener
{
    private final Fixture playerFixture;
    private final Fixture sensorFixture;
    private Fixture attackFixture;
    private final FixtureDef attackFixtureDef;

    private final IntSet collidingEnemies = new IntSet();

    private boolean isSkillWindowOpen = false;

    private final Vector2 tempVector2 = new Vector2();
    private final Vector3 tempVector3 = new Vector3();

    private final Vector2 attackOffset = new Vector2();
    private final float attackFadeTime = 0.2f;
    private float remainingAttackFade = 0;
    boolean isAttacking = false;
    boolean isWalking = false;
    TileRoom roomEntered = null;
    TileRoom currentRoom = null;
    DoorScript doorReached = null;

    int availableSkillPoints = 0;

    public PlayerScript(Fixture playerFixture, FixtureDef attackFixtureDef, Fixture sensorFixture)
    {
        this.playerFixture = playerFixture;
        this.attackFixtureDef = attackFixtureDef;
        this.sensorFixture = sensorFixture;

        Engine.eventBus.subscribe(this, SkillPointUsedEvent.class);
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

        if(!tempVector2.isZero())
        {
            tempVector2.setLength(1);
            rotateSprite(tempVector2);
            if(!isWalking)
            {
                getComponent(MultiAnimationComponent.class).animationSets.get(0).play(PlayerAnimationId.WALK);
                if(isSkillWindowOpen)
                {
                    isSkillWindowOpen = false;
                    Engine.eventBus.publish(SkillWindowToggleEvent.class, e -> e.show = false);
                }
                isWalking = true;
            }
        }
        else if(isWalking)
        {
            getComponent(MultiAnimationComponent.class).animationSets.get(0).play(PlayerAnimationId.IDLE);
            isWalking = false;
        }

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
            getComponent(MultiSpriteComponent.class).sprites.get(1).setAlpha(remainingAttackFade/attackFadeTime);
        }

        if(roomEntered != null)
        {
            Engine.eventBus.publish(EnterRoomEvent.class, e -> e.room = roomEntered);
            currentRoom = roomEntered;
            roomEntered = null;
        }

        if(collidingEnemies.size > 0)
        {
            Vector2 otherPosition = getScene().getComponentStorage(TransformComponent.class).getByEntity(collidingEnemies.first()).position;
            dealDamage(1, otherPosition);
        }
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, ENEMY) && isCategory(category, PLAYER))
        {
            collidingEnemies.add(otherEntity);
        }
        else if(isCategory(otherCategory, OBSTACLE) && isCategory(category, PLAYER))
        {
            ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(otherEntity);
            if(scriptComponent == null)
                return;
            Script script = scriptComponent.script;
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
        else if(isCategory(otherCategory, OBSTACLE) && isCategory(category, SENSOR))
        {
            ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(otherEntity);
            if(scriptComponent == null)
                return;
            Script script = scriptComponent.script;
            if(script instanceof DoorScript doorScript)
            {
                doorReached = doorScript;
            }
        }
    }

    @Override
    public void onEndCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, ENEMY) && isCategory(category, PLAYER))
        {
            collidingEnemies.remove(otherEntity);
        }
        else if(isCategory(otherCategory, OBSTACLE) && isCategory(category, SENSOR))
        {
            ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(otherEntity);
            if(scriptComponent == null)
                return;
            Script script = scriptComponent.script;
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
        else if(keycode == Input.Keys.X && !isWalking)
        {
            tempVector3.set(getComponent(TransformComponent.class).position, 0);
            getScene().getActiveCamera().project(tempVector3);
            Engine.eventBus.publish(SkillWindowToggleEvent.class, e ->
            {
                e.show = !isSkillWindowOpen;
                e.xPos = tempVector3.x;
                e.yPos = tempVector3.y;
            });
            isSkillWindowOpen = !isSkillWindowOpen;
        }
        else if(keycode == Input.Keys.Z)
        {
            addSkillPoint();
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

    public void addSkillPoint()
    {
        availableSkillPoints++;
        Engine.eventBus.publish(SkillPointsAvailableEvent.class, e -> e.skillPoints = availableSkillPoints);
    }

    private void dealDamage(float damage, Vector2 damageSource)
    {
        HealthComponent health = getComponent(HealthComponent.class);
        if(health.dealDamage(damage))
        {
            Engine.eventBus.publish(HealthChangeEvent.class, e -> e.currentHealth = health.health);
            Body body = getComponent(PhysicalBodyComponent.class).body;
            Vector2 push = tempVector2.set(body.getPosition()).sub(damageSource).scl(24);
            body.applyLinearImpulse(push, body.getPosition(), true);
            if(health.health <= 0)
            {
                MenuBuilder menuBuilder = new MenuBuilder();
                Scene scene = getEngine().createScene();
                menuBuilder.build(getEngine(), scene);
                getEngine().switchScene(scene);
            }
        }
    }

    private void attack(Direction direction)
    {
        if(isAttacking)
            return;
        attackOffset.set(direction.getDeltaX()*0.75f, -1 * direction.getDeltaY()*0.75f);

        PolygonShape attackShape = (PolygonShape) attackFixtureDef.shape;
        attackShape.setAsBox(0.6f, 0.6f, attackOffset, 0);
        isAttacking = true;
        remainingAttackFade = attackFadeTime;
        attackFixture = getScene().createFixture(getEntity(), attackFixtureDef);
        Sprite sprite = getComponent(MultiSpriteComponent.class).sprites.get(1);
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

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof SkillPointUsedEvent skillPointEvent)
        {
            availableSkillPoints--;
            SkillPointsAvailableEvent pointsAvailableEvent = Engine.poolManager.obtain(SkillPointsAvailableEvent.class);
            pointsAvailableEvent.skillPoints = availableSkillPoints;
            Engine.eventBus.publish(SkillPointsAvailableEvent.class, e -> e.skillPoints = availableSkillPoints);
            Engine.poolManager.free(pointsAvailableEvent);
        }
    }

    private void stopAttack()
    {
        if(attackFixture != null)
            getScene().setFixtureToDelete(attackFixture);
        isAttacking = false;
    }

    private void rotateSprite(Vector2 vector)
    {
        Sprite playerSprite = getComponent(MultiSpriteComponent.class).sprites.get(0);
        float degrees = vector.angleDeg();
        playerSprite.setRotation(degrees);
    }
}
