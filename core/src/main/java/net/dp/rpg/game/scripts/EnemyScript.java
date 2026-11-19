package net.dp.rpg.game.scripts;

import com.badlogic.gdx.graphics.Color;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.MultiSpriteComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.appearanceEnums.PlayerSpriteId;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.level.events.EnemyKilledEvent;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.item.PlayerStats;

public class EnemyScript extends EntityScript
{
    protected final int playerEntity;
    protected final TileFloor floor;
    protected final TileRoom home;
    protected final EnemyDef enemy;
    private final DialogueController dialogues;
    private boolean dead = false;

    private final float damageFlashDuration = 0.2f;
    private float damageFlashTimer = 0;

    protected SpriteComponent mainSpriteComponent;
    protected final Color defaultColor = new Color(Color.WHITE);
    protected final Color damageColor = new Color(Color.RED);

    public EnemyScript(int playerEntity, EnemyDef enemy, TileFloor floor, TileRoom home, DialogueController dialogues)
    {
        this.dialogues = dialogues;
        this.playerEntity = playerEntity;
        this.enemy = enemy;
        this.floor = floor;
        this.home = home;
    }

    public EnemyDef getEnemy()
    {
        return enemy;
    }

    @Override
    public void onStart()
    {
        mainSpriteComponent = getComponent(SpriteComponent.class);
        if(mainSpriteComponent != null)
            mainSpriteComponent.sprite.setColor(defaultColor);
    }

    @Override
    public final void update(float delta)
    {
        // Enemies of other rooms sleep; otherwise they would walk into walls towards the player.
        if(floor != null && home != null && floor.getActiveRoom() != home)
            return;
        // The world waits while the player reads.
        if(dialogues != null && dialogues.isOpen())
            return;

        if(damageFlashTimer > 0)
        {
            damageFlashTimer -= delta;
            if(damageFlashTimer <= 0)
            {
                mainSpriteComponent.sprite.setColor(defaultColor);
            }
        }

        enemyUpdate(delta);
    }

    protected void enemyUpdate(float delta)
    {
    }

    // CombatSystem deals the player's sword damage (AttackComponent); this only reacts to dying.
    // A killing blow can land twice in one swing, hence the guard.
    @Override
    public void onDeath()
    {
        if(dead)
            return;
        dead = true;
        getScene().setToDelete(getEntity());
        Engine.eventBus.publish(EnemyKilledEvent.class, e ->
        {
            e.enemy = enemy;
            e.room = home;
        });
        if(enemy.boss())
            Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = enemy.name() + " pokonany!");
    }

    @Override
    public void onDamage()
    {
        damageFlashTimer = damageFlashDuration;
        mainSpriteComponent.sprite.setColor(damageColor);
    }

    protected float getSpeed()
    {
        // Acceleration, not force, so a heavy boss is not slower than its numbers say.
        return enemy.acceleration() * getComponent(PhysicalBodyComponent.class).body.getMass();
    }

    protected PlayerStats playerStats()
    {
        ScriptComponent scriptComponent = getScene().getComponentStorage(ScriptComponent.class).getByEntity(playerEntity);
        if(scriptComponent != null && scriptComponent.script instanceof PlayerScript playerScript)
            return playerScript.getStats();
        return null;
    }
}
