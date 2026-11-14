package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.UiConfig;
import net.dp.rpg.game.level.events.HealthChangeEvent;

public class HealthBar extends ProgressBar implements GameEventListener
{
    private final float scale = UiConfig.HEALTH_BAR_SCALE;
    private boolean knobBeforeDisable = false;

    public HealthBar(float max, Skin skin)
    {
        super(0, max, 1, false, skin, "health-bar");
        getBackgroundDrawable().setMinHeight(8*scale);
        getKnobBeforeDrawable().setMinHeight(6*scale);
        getBackgroundDrawable().setMinWidth((max+2)*scale);
        getKnobBeforeDrawable().setMinWidth(100);
        setWidth((max+2)*scale);
        setStepSize(1);
        layout();
    }

    public void update(float value)
    {
        if(value <= 1)
            knobBeforeDisable = true;
        else
            knobBeforeDisable = false;
        this.setValue(value);
    }

    @Override
    protected Drawable getKnobBeforeDrawable ()
    {
        if(knobBeforeDisable)
            return null;
        return super.getKnobBeforeDrawable();
    }

    @Override
    public void layout()
    {
        setWidth((getMaxValue()+2)*scale);
        float maxWidth = 102*scale;
        if(getWidth() > maxWidth)
        {
            setWidth(maxWidth - (maxWidth % scale));
            setStepSize(getMaxValue() / (getWidth()/8-2));
        }
        update(getValue());
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
