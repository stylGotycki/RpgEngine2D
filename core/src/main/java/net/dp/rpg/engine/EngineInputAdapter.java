package net.dp.rpg.engine;

import com.badlogic.gdx.InputAdapter;
import com.badlogic.gdx.utils.Array;

public class EngineInputAdapter extends InputAdapter
{
    private final Array<InputListener> listeners = new Array<>();

    public void create()
    {
    }

    public void addListener(InputListener listener)
    {
        if(listener != null)
            listeners.add(listener);
    }

    public void removeListener(InputListener listener)
    {
        if(listener != null)
            listeners.removeValue(listener, true);
    }

    @Override
    public boolean keyDown (int keycode)
    {
        for(InputListener listener : listeners)
        {
            if(listener.keyDown(keycode))
                return true;
        }
        return false;
    }

    @Override
    public boolean keyUp (int keycode)
    {
        for(InputListener listener : listeners)
        {
            if(listener.keyUp(keycode))
                return true;
        }
        return false;
    }
}
