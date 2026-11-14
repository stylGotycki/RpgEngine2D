package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.math.Vector3;
import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import net.dp.rpg.engine.*;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.systems.input.InputListener;
import net.dp.rpg.engine.tile.room.Direction;
import static net.dp.rpg.game.FixtureCategory.*;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.dialogue.GameDialogues;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.item.ItemCategory;
import net.dp.rpg.game.item.ItemDef;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.item.Stat;
import net.dp.rpg.game.level.entities.ItemPickupCreator;
import net.dp.rpg.game.animations.PlayerAnimationId;
import net.dp.rpg.game.level.events.*;
import java.util.Optional;

public class PlayerScript extends EntityScript implements InputListener, GameEventListener
{
    private final Fixture playerFixture;
    private final Fixture sensorFixture;
    private Fixture attackFixture;
    private final FixtureDef attackFixtureDef;

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

    private float lastDamage = 0;
    private String lastDamageSource = "enemy";
    private final PlayerStats stats;
    private final DialogueController dialogues;
    private ItemPickupScript itemReached = null;
    private NpcScript npcReached = null;
    private float attackCooldown = 0;

    public PlayerScript(Fixture playerFixture, FixtureDef attackFixtureDef, Fixture sensorFixture, PlayerStats stats, DialogueController dialogues)
    {
        this.dialogues = dialogues;
        this.playerFixture = playerFixture;
        this.attackFixtureDef = attackFixtureDef;
        this.sensorFixture = sensorFixture;
        this.stats = stats;

        Engine.eventBus.subscribe(this, SkillPointUsedEvent.class);
        Engine.eventBus.subscribe(this, EnterRoomEvent.class);
    }

    @Override
    public void update(float delta)
    {
        stats.update(delta);
        if(attackCooldown > 0)
            attackCooldown -= delta;
        float speed = 60f * stats.moveSpeedMultiplier();
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

        // The player stands still while talking.
        if(dialogues.isOpen())
            tempVector2.set(0, 0);

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
            }
            getComponent(MultiSpriteComponent.class).sprites.get(1).setAlpha(remainingAttackFade/attackFadeTime);
        }

        if(isAttacking)
        {
            stopAttack();
        }

        if(roomEntered != null)
        {
            Engine.eventBus.publish(EnterRoomEvent.class, e -> e.room = roomEntered);
            currentRoom = roomEntered;
            roomEntered = null;
        }
    }

    @Override
    public void onCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, ITEM) && isCategory(category, PLAYER))
        {
            if(scriptOf(otherEntity) instanceof ItemPickupScript pickup)
            {
                itemReached = pickup;
                hover(pickup);
            }
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
                // The door belongs to the room being entered and sits in its wall facing the player,
                // so the tile in front of it, against its side, is inside that room. Measured from the
                // door, not from the player: one tile from the player left the body overlapping the
                // door, which closes on entering.
                Direction doorDirection = doorScript.getRoomEdge().opposite();
                Utils.directionToVector2(doorDirection, tempVector2).add(doorScript.getCenter());
                transformComponent.setPosition(tempVector2);
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
        else if(isCategory(otherCategory, NPC) && isCategory(category, SENSOR))
        {
            if(scriptOf(otherEntity) instanceof NpcScript npc)
            {
                npcReached = npc;
                message("[[E] Porozmawiaj: " + npc.name());
            }
        }
    }

    @Override
    public void onEndCollision(int otherEntity, short category, short otherCategory)
    {
        if(isCategory(otherCategory, ITEM) && isCategory(category, PLAYER))
        {
            if(itemReached != null && itemReached.getEntity() == otherEntity)
            {
                itemReached = null;
                hover(null);
            }
        }
        else if(isCategory(otherCategory, NPC) && isCategory(category, SENSOR))
        {
            if(npcReached != null && npcReached.getEntity() == otherEntity)
                npcReached = null;
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
        if(dialogues.isOpen())
        {
            if(keycode == Input.Keys.E || keycode == Input.Keys.SPACE || keycode == Input.Keys.ENTER)
                dialogues.confirm();
            else if(keycode == Input.Keys.W || keycode == Input.Keys.UP)
                dialogues.moveSelection(-1);
            else if(keycode == Input.Keys.S || keycode == Input.Keys.DOWN)
                dialogues.moveSelection(1);
            else if(keycode >= Input.Keys.NUM_1 && keycode <= Input.Keys.NUM_3)
                dialogues.choose(keycode - Input.Keys.NUM_1);
            return true;
        }

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
            if(itemReached != null)
                pickUp(itemReached);
            else if(npcReached != null)
                npcReached.talk(dialogues, GameDialogues.library());
            else if(doorReached != null)
            {
                if(!doorReached.open())
                    Gdx.app.log("Door", "Locked. K unlocks it (debug, until keys exist).");
            }
            else
            {
                // Nothing to pick up or talk to: whatever stands here (a circuit piece) may react.
                Vector2 at = getComponent(TransformComponent.class).position;
                Engine.eventBus.publish(InteractEvent.class, e ->
                {
                    e.x = at.x;
                    e.y = at.y;
                });
            }
        }
        else if(keycode == Input.Keys.NUM_1 || keycode == Input.Keys.NUM_2 || keycode == Input.Keys.NUM_3)
        {
            usePotion(keycode - Input.Keys.NUM_1);
        }
        else if(keycode == Input.Keys.K)
        {
            if(doorReached != null)
            {
                doorReached.unlock();
                doorReached.open();
            }
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

    @Override
    public void onDamage()
    {
        HealthComponent health = getComponent(HealthComponent.class);
        Engine.eventBus.publish(HealthChangeEvent.class, e -> e.currentHealth = health.health);
        // For the adaptation metrics and the CHALLENGE quests.
        float amount = lastDamage;
        String source = lastDamageSource;
        TileRoom room = currentRoom;
        Engine.eventBus.publish(PlayerDamagedEvent.class, e ->
        {
            e.amount = amount;
            e.source = source;
            e.room = room;
        });
    }

    @Override
    public void onDeath()
    {
        getComponent(MultiAnimationComponent.class).animationSets.get(0).play(PlayerAnimationId.DEATH);
        Engine.eventBus.publish(PlayerDeathEvent.class, e -> e.playerId = getEntity());
    }

    public void addSkillPoint()
    {
        availableSkillPoints++;
        Engine.eventBus.publish(SkillPointsAvailableEvent.class, e -> e.skillPoints = availableSkillPoints);
    }

    // CombatSystem deals the damage; armour scales it here and onDamage reports it.
    @Override
    public float modifyIncomingDamage(int attacker, float damage)
    {
        EnemyDef enemy = scriptOf(attacker) instanceof EnemyScript enemyScript ? enemyScript.getEnemy() : null;
        lastDamageSource = enemy == null ? "enemy" : enemy.id();
        lastDamage = stats.damageTaken(damage);
        return lastDamage;
    }
    private void attack(Direction direction)
    {
        if(isAttacking || attackCooldown > 0)
            return;
        attackCooldown = stats.attackCooldown();
        // CombatSystem reads the sword's damage from here: weapon and strength count.
        getComponent(AttackComponent.class).damage = stats.damage();
        attackOffset.set(direction.getDeltaX()*0.75f, direction.getDeltaY()*0.75f);

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
        if(event instanceof EnterRoomEvent enterRoomEvent)
        {
            currentRoom = enterRoomEvent.room;
        }
        else if(event instanceof SkillPointUsedEvent skillPointEvent)
        {
            Stat stat = Stat.of(skillPointEvent.skillType);
            stats.setBase(stat, Math.max(stats.base(stat), skillPointEvent.level));
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
        getComponent(AttackComponent.class).clearHitEntities();
    }

    private void rotateSprite(Vector2 vector)
    {
        Sprite playerSprite = getComponent(MultiSpriteComponent.class).sprites.get(0);
        float degrees = vector.angleDeg();
        playerSprite.setRotation(degrees);
    }

    public PlayerStats getStats()
    {
        return stats;
    }

    private Script scriptOf(int entity)
    {
        ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(entity);
        return scriptComponent == null ? null : scriptComponent.script;
    }

    private void pickUp(ItemPickupScript pickup)
    {
        ItemDef item = pickup.item();
        if(pickup.isLocked())
        {
            message(pickup.lockedMessage());
            return;
        }
        if(item.category() == ItemCategory.QUEST)
        {
            // Quest items take no slot; the quest tracker counts them.
        }
        else if(item.isEquipment())
        {
            Optional<String> missing = stats.missingRequirement(item);
            if(missing.isPresent())
            {
                message("Wymaga: " + missing.get());
                return;
            }
            // The old piece lands where the player stands, so a swap can be undone.
            stats.equip(item).ifPresent(this::drop);
        }
        else if(stats.addPotion(item) < 0)
        {
            message("Brak miejsca na miksturę (1, 2, 3)");
            return;
        }

        removePickups(pickup);
        itemReached = null;
        hover(null);
        boolean fromShop = pickup.group() != null;
        String questId = pickup.questId();
        Engine.eventBus.publish(ItemPickedEvent.class, e ->
        {
            e.item = item;
            e.fromShop = fromShop;
            e.questId = questId;
        });
        message("Podniesiono: " + item.name());
    }

    private void drop(ItemDef item)
    {
        ItemPickupCreator creator = new ItemPickupCreator(getScene(), getEngine().getAssetManager().get("atlases/textures.atlas", TextureAtlas.class));
        Vector2 position = getComponent(TransformComponent.class).position;
        creator.create(item, position.x, position.y, null);
        creator.dispose();
    }

    private void removePickups(ItemPickupScript taken)
    {
        getScene().setToDelete(taken.getEntity());
        if(taken.group() == null)
            return;
        ComponentStorage<ScriptComponent> scripts = getScene().getComponentStorage(ScriptComponent.class);
        for(int i = 0; i < scripts.size(); i++)
        {
            if(scripts.getByIndex(i).script instanceof ItemPickupScript other && other != taken
                    && taken.group().equals(other.group()))
                getScene().setToDelete(other.getEntity());
        }
    }

    private void usePotion(int slot)
    {
        ItemDef potion = stats.potion(slot);
        if(potion == null)
            return;
        float heal = stats.usePotion(slot);
        if(heal > 0)
        {
            HealthComponent health = getComponent(HealthComponent.class);
            health.addHealth(heal);
            Engine.eventBus.publish(HealthChangeEvent.class, e -> e.currentHealth = health.health);
        }
        Engine.eventBus.publish(PotionUsedEvent.class, e ->
        {
            e.potion = potion;
            e.slot = slot;
        });
        message(potion.name());
    }

    private void hover(ItemPickupScript pickup)
    {
        Engine.eventBus.publish(ItemHoverEvent.class, e ->
        {
            e.item = pickup == null ? null : pickup.item();
            e.fromShop = pickup != null && pickup.group() != null;
        });
    }

    private void message(String text)
    {
        Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = text);
    }
}
