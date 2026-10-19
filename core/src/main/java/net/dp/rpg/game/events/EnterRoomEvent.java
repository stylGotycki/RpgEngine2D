package net.dp.rpg.game.events;

import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.game.TileRoom;

public class EnterRoomEvent implements GameEvent
{
    public TileRoom room;

    @Override
    public void reset()
    {
        room = null;
    }
}
