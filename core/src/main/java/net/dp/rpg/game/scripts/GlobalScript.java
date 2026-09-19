package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import net.dp.rpg.engine.InputListener;
import net.dp.rpg.engine.Script;

public class GlobalScript extends Script implements InputListener
{
    @Override
    public void update(float delta)
    {

    }

    @Override
    public boolean keyDown(int keyCode)
    {
        if(keyCode == Input.Keys.ESCAPE)
        {
            getEngine().exit();
        }
        if(keyCode == Input.Keys.F11)
        {
            if(Gdx.graphics.isFullscreen())
                Gdx.graphics.setWindowedMode(1280, 720);
            else
                Gdx.graphics.setFullscreenMode(Gdx.graphics.getDisplayMode());
        }
        else
            return false;

        return true;
    }

    @Override
    public boolean keyUp(int keyCode)
    {
        return false;
    }
}
