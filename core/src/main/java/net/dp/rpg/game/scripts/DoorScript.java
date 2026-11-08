package net.dp.rpg.game.scripts;

import com.badlogic.gdx.physics.box2d.Fixture;
import lombok.Getter;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.tile.room.Direction;
import net.dp.rpg.game.TileRoom;

public class DoorScript extends EntityScript
{
    @Getter
    private final TileRoom room;
    @Getter
    private final Direction roomEdge;
    @Getter
    private boolean closed = true;
    private final Fixture fixture;

    public DoorScript(TileRoom room, Direction roomEdge, Fixture fixture)
    {
        this.room = room;
        this.roomEdge = roomEdge;
        this.fixture = fixture;
    }

    @Override
    public void update(float delta)
    {

    }

    public void open()
    {
        closed = false;
        fixture.setSensor(true);
    }

    public void close()
    {
        closed = true;
        fixture.setSensor(false);
    }
}
