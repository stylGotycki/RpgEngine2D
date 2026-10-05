package net.dp.rpg.game.level;

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;

public class HealthBar extends ProgressBar implements GameEventListener
{
    public HealthBar(float max, Skin skin)
    {
        super(0, max, 1, false, skin, "health-bar");
        update(100);
        getBackgroundDrawable().setMinHeight(40);
        getKnobBeforeDrawable().setMinHeight(32);
    }

    private void update(float value)
    {
        this.setValue(value);
    }

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof HealthChangeEvent change)
        {
            update(change.currentHealth);
        }
    }
}
