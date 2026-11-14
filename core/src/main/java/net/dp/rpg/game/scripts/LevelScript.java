package net.dp.rpg.game.scripts;

import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.SceneScript;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.level.events.PlayerDeathEvent;
import net.dp.rpg.game.menu.MenuBuilder;

public class LevelScript extends SceneScript implements GameEventListener
{
    float afterPlayerDeadTime = 1;
    boolean playerDeath = false;

    @Override
    public void update(float delta)
    {
        if(playerDeath)
        {
            afterPlayerDeadTime -= delta;
            if(afterPlayerDeadTime <= 0)
            {
                MenuBuilder menuBuilder = new MenuBuilder();
                Scene scene = getEngine().createScene();
                menuBuilder.build(getEngine(), scene);
                getEngine().switchScene(scene);
            }
        }
    }

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof PlayerDeathEvent e)
        {
            playerDeath = true;
        }
    }
}
