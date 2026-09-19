package net.dp.rpg.game.menu;

import com.badlogic.gdx.Input;
import net.dp.rpg.engine.InputListener;
import net.dp.rpg.engine.SceneScript;

public class MenuScript extends SceneScript implements InputListener
{
    private final ButtonTable buttons;

    public MenuScript(ButtonTable buttonTable)
    {
        this.buttons = buttonTable;
    }

    @Override
    public void update(float delta)
    {
    }

    @Override
    public boolean keyDown(int keyCode)
    {
        if(keyCode == Input.Keys.DOWN)
        {
            buttons.setNextActive();
        }
        else if(keyCode == Input.Keys.UP)
        {
            buttons.setPreviousActive();
        }
        else if(keyCode == Input.Keys.ENTER)
        {
            buttons.clickActive();
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
