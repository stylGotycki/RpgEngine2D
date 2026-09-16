package net.dp.rpg.game.scripts;

import com.badlogic.gdx.Gdx;
import net.dp.rpg.engine.Script;

public class AppCloseScript extends Script
{
    private final int closeKey;

    public AppCloseScript(int closeKey)
    {
        this.closeKey = closeKey;
    }

    @Override
    public void update(float delta)
    {
        if(Gdx.input.isKeyJustPressed(closeKey))
        {
            getEngine().exit();
        }
    }
}
