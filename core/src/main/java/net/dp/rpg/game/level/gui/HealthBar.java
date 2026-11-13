package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.level.events.HealthChangeEvent;

public class HealthBar extends ProgressBar implements GameEventListener
{
    private final float scale = 8;
    private final Drawable fillDrawable;

    public HealthBar(float max, Skin skin)
    {
        super(0, max, 1, false, skin, "health-bar");
        fillDrawable = getKnobBeforeDrawable();
        NinePatchDrawable patchDrawable = (NinePatchDrawable)getStyle().background;
        NinePatch patch = patchDrawable.getPatch();
        patch.scale(scale,scale);
        patchDrawable.setPatch(patch);
        patchDrawable = (NinePatchDrawable)getStyle().knobBefore;
        patch = patchDrawable.getPatch();
        patch.scale(scale,scale);
        patchDrawable.setPatch(patch);
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
            getStyle().knobBefore = null;
        else
            getStyle().knobBefore = fillDrawable;
        this.setValue(value);
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
