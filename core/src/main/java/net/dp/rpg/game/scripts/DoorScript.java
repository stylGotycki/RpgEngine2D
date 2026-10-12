package net.dp.rpg.game.scripts;

import lombok.Getter;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.game.TileRoom;

public class DoorScript extends EntityScript
{
    @Getter
    private final TileRoom room;

    public DoorScript(TileRoom room)
    {
        this.room = room;
    }

    @Override
    public void update(float delta)
    {

    }
}
