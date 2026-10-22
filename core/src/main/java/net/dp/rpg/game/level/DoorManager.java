package net.dp.rpg.game.level;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.events.EnterRoomEvent;
import net.dp.rpg.game.scripts.DoorScript;

public class DoorManager implements GameEventListener
{
    private final IntMap<Array<DoorScript>> roomDoors = new IntMap<>();

    public void addDoor(int roomId, DoorScript door)
    {
        if(!roomDoors.containsKey(roomId))
            roomDoors.put(roomId, new Array<>());
        roomDoors.get(roomId).add(door);
    }

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof EnterRoomEvent roomEvent)
        {
            if(roomDoors.get(roomEvent.room.id()) == null)
                return;
            for(DoorScript door : roomDoors.get(roomEvent.room.id()))
            {
                door.close();
            }
        }
    }
}
