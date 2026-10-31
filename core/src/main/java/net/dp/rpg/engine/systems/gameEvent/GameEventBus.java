package net.dp.rpg.engine.systems.gameEvent;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ObjectMap;
import com.badlogic.gdx.utils.PoolManager;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.game.events.EnterRoomEvent;

import java.util.function.Consumer;

public class GameEventBus
{
    private final ObjectMap<Class<? extends GameEvent>, Array<GameEventListener>> listeners = new ObjectMap<>();
    private final PoolManager poolManager;

    public GameEventBus(PoolManager poolManager)
    {
        this.poolManager = poolManager;
    }

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

    public <T extends GameEvent> void publish(Class<T> eventClass, Consumer<T> init)
    {
        T event = poolManager.obtain(eventClass);
        init.accept(event);
        if(listeners.containsKey(event.getClass()))
        {
            for(GameEventListener listener : listeners.get(event.getClass()))
            {
                listener.onEvent(event);
            }
        }
        Engine.poolManager.free(event);
    }
}
