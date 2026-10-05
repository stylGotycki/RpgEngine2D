package net.dp.rpg.engine.systems.gameEvent;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;

public class GameEventBus
{
    private final ObjectMap<Class<? extends GameEvent>, Array<GameEventListener>> listeners = new ObjectMap<>();

    public void subscribe(GameEventListener listener, Class<? extends GameEvent> eventClass)
    {
        Array<GameEventListener> listenersArray = listeners.get(eventClass);
        if(listenersArray == null)
        {
            listenersArray = new Array<>();
            listeners.put(eventClass, listenersArray);
        }
        listenersArray.add(listener);
    }

    public void unsubscribe(GameEventListener listener, GameEvent event)
    {
        Array<GameEventListener> listenersArray = listeners.get(event.getClass());
        if(listenersArray != null)
        {
            listenersArray.removeValue(listener, true);
        }
    }

    public void publish(GameEvent event)
    {
        if(listeners.containsKey(event.getClass()))
        {
            for(GameEventListener listener : listeners.get(event.getClass()))
            {
                listener.onEvent(event);
            }
        }
    }
}
