package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.WidgetGroup;
import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.SkillType;
import net.dp.rpg.game.level.events.SkillPointsAvailableEvent;
import net.dp.rpg.game.level.events.SkillWindowToggleEvent;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.item.Stat;

public class SkillWindow extends WidgetGroup implements GameEventListener
{
    Array<SkillField> skillFields = new Array<>();
    private float xPos = 0;
    private float yPos = 0;

    public SkillWindow(Skin skin, int maxSkillLevel)
    {
        for(SkillType type : SkillType.values())
        {
            SkillField currentParent = null;
            for(int level = 1; level <= maxSkillLevel; level++)
            {
                SkillField skillField = new SkillField(skin, type, level, currentParent);
                skillField.setTouchable(Touchable.disabled);
                skillFields.add(skillField);
                addActor(skillField);
                currentParent = skillField;
            }
        }
    }

    // Marks the points the player already has (rolled at start, or spent on an earlier floor),
    // without publishing SkillPointUsedEvent for them.
    public void showLevels(PlayerStats stats)
    {
        for(SkillField field : skillFields)
        {
            if(field.skillLevel <= stats.base(Stat.of(field.skillType)))
            {
                field.setProgrammaticChangeEvents(false);
                field.setChecked(true);
                field.setProgrammaticChangeEvents(true);
                field.setTouchable(Touchable.disabled);
            }
        }
    }

    @Override
    public void layout()
    {
        double angleStep = 2 * Math.PI / SkillType.values().length;
        for(SkillField skillField : skillFields)
        {
            float size = Gdx.graphics.getWidth()/48f;
            skillField.setSize(size, size);

//            float centerX = Gdx.graphics.getWidth() / 2f;
//            float centerY = Gdx.graphics.getHeight() / 2f;
            int radius = skillField.skillLevel * (int)size;
            double angle = skillField.skillType.ordinal() * angleStep;

            skillField.setPosition(xPos + (float)Math.sin(angle) * radius - size/2, yPos + (float)Math.cos(angle) * radius - size/2);
        }
    }

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof SkillWindowToggleEvent toggleEvent)
        {
            xPos = toggleEvent.xPos;
            yPos = toggleEvent.yPos;
            invalidate();
            this.setVisible(toggleEvent.show);
        }
        if(event instanceof SkillPointsAvailableEvent skillEvent)
        {
            for(SkillField field : skillFields)
            {
                field.updateState(skillEvent.skillPoints);
            }
        }
    }
}
