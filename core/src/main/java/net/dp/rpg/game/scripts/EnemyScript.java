package net.dp.rpg.game.scripts;

import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.enemy.EnemyDef;
import net.dp.rpg.game.level.events.EnemyKilledEvent;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.item.PlayerStats;

public class EnemyScript extends EntityScript
{
    protected final int playerEntity;
    private final TileFloor floor;
    private final TileRoom home;
    protected final EnemyDef enemy;
    private final DialogueController dialogues;
    private boolean dead = false;

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
    public final void update(float delta)
    {
        // Enemies of other rooms sleep; otherwise they would walk into walls towards the player.
        if(floor != null && home != null && floor.getActiveRoom() != home)
            return;
        // The world waits while the player reads.
        if(dialogues != null && dialogues.isOpen())
            return;

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
