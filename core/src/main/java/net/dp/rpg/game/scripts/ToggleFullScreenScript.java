package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import net.dp.rpg.engine.Script;

public class ToggleFullScreenScript extends Script
{
    @Override
    public void update(float delta)
    {
        if (Gdx.input.isKeyJustPressed(Input.Keys.F11))
        {
            if(Gdx.graphics.isFullscreen())
                Gdx.graphics.setWindowedMode(1280, 720);
            else
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
    }
}
