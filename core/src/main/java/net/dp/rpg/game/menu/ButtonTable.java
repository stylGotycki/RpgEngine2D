package net.dp.rpg.game.menu;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.utils.Array;
import lombok.Getter;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.game.LevelBuilder;

public class ButtonTable extends Table implements ButtonStateListener
{
    @Getter
    private final Array<ParticleButton> buttons = new Array<>();
    private int active = -1;

    private final Skin skin;
    private final ParticleEffect particle;

    public ButtonTable(EngineServices engine, Skin skin, ParticleEffect particle)
    {
        this.skin = skin;
        this.particle = particle;
        LevelBuilder levelBuilder = new LevelBuilder();

        addButton("Play", new ChangeListener()
        {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                Scene scene = engine.createScene();
                levelBuilder.build(engine, scene);
                engine.switchScene(scene);
            }
        });

        addButton("Exit", new ChangeListener()
        {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                engine.exit();
            }
        });
    }

    public void setNextActive()
    {
        if(active < 0)
        {
            active = 0;
            enter(active);
        }
        else
        {
            exit(active);
            active = ( active+1 ) % buttons.size;
            enter(active);
        }
    }

    public void setPreviousActive()
    {
        if(active < 0)
        {
            active = 0;
            enter(active);
        }
        else
        {
            exit(active);
            active = (active == 0 ? buttons.size-1 : active-1);
            enter(active);
        }
    }

    public void clickActive()
    {
        if(active >= 0)
        {
            buttons.get(active).getButton().fire(new ChangeListener.ChangeEvent());
        }
    }

    @Override
    public void enter(int buttonId)
    {
        resetActive();
        buttons.get(buttonId).enter();
        active = buttonId;
    }

    @Override
    public void exit(int buttonId)
    {
        buttons.get(buttonId).exit();
    }

    private void resetActive()
    {
        if(active >= 0)
            buttons.get(active).exit();
        active = -1;
    }

    private void addButton(String name, ChangeListener changeListener)
    {
        if(buttons.size > 0)
            this.row();

        ParticleButton button = new ParticleButton(name, new ParticleEffect(particle), skin);
        button.setListener(this, buttons.size);
        button.addListener(changeListener);
        buttons.add(button);

        this.add(button).width(200).pad(10);
    }
}
