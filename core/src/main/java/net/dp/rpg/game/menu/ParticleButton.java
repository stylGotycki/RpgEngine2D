package net.dp.rpg.game.menu;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import lombok.Getter;
import lombok.Setter;

public class ParticleButton extends Stack
{
    @Getter
    private final TextButton button;
    @Getter
    private final ParticleEffectActor particleActor;

    private int id;
    private ButtonStateListener stateListener;

    public ParticleButton(String name, ParticleEffect particle, Skin skin)
    {
        particleActor = new ParticleEffectActor(particle, false);
        particleActor.setTouchable(Touchable.disabled);

        button = new TextButton(name, skin);

        ParticleButton particleButton = this;

        button.addListener(new ClickListener()
        {
            @Override
            public void enter(InputEvent event, float x, float y, int pointer, Actor fromActor)
            {
                if(stateListener != null)
                    stateListener.enter(id);
                particleButton.enter();
            }

            @Override
            public void exit(InputEvent event, float x, float y, int pointer, Actor toActor)
            {
                if(stateListener != null)
                    stateListener.exit(id);
                particleButton.exit();
            }
        });

        this.add(button);
        this.add(particleActor);
    }

    @Override
    public void layout()
    {
        super.layout();
        particleActor.setPosition(button.getX() + button.getWidth() / 2, button.getY() + button.getHeight() / 2);
        particleActor.getEffect().getEmitters().first().getSpawnWidth().setHigh(button.getWidth());
        particleActor.getEffect().getEmitters().first().getSpawnHeight().setHigh(button.getHeight());

        //to prevent too many particles spawning after first enter
        particleActor.start();
        particleActor.allowCompletion();
    }

    public void setListener(ButtonStateListener listener, int buttonId)
    {
        stateListener = listener;
        id = buttonId;
    }

    public void enter()
    {
        particleActor.start();
    }

    public void exit()
    {
        particleActor.allowCompletion();
    }
}
