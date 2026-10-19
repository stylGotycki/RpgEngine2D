package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ImageButton;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.game.SkillType;
import net.dp.rpg.game.events.SkillPointUsedEvent;

public class SkillField extends ImageButton
{
    int skillLevel;
    SkillType skillType;
    SkillField parent;

    public SkillField(Skin skin, SkillType skillType, int skillLevel, SkillField parent)
    {
        super(skin, "skill-" + skillType.getColorString());

        this.skillType = skillType;
        this.skillLevel = skillLevel;
        this.parent = parent;

        getImageCell().expand().fill();

        addListener(new ChangeListener()
        {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                actor.setTouchable(Touchable.disabled);
                SkillPointUsedEvent skillPointEvent = Engine.poolManager.obtain(SkillPointUsedEvent.class);
                skillPointEvent.level = skillLevel;
                skillPointEvent.skillType = skillType;
                Engine.eventBus.publish(skillPointEvent);
                Engine.poolManager.free(skillPointEvent);
            }
        });
    }

    public void updateState(int availablePoints)
    {
        if((parent == null || parent.isChecked()) && availablePoints > 0)
            this.setTouchable(Touchable.enabled);
        else
            this.setTouchable(Touchable.disabled);
    }
}
