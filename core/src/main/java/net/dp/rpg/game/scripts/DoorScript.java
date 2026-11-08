package net.dp.rpg.game.scripts;

import com.badlogic.gdx.math.Vector2;
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
    /** Locked doors come from the floor graph (DoorType.LOCKED); a key will unlock them. */
    @Getter
    private boolean locked;
    private final Fixture fixture;
    /** World centre of the door tile; the player lands one tile further into the room. */
    @Getter
    private final Vector2 center;

    public DoorScript(TileRoom room, Direction roomEdge, Fixture fixture, boolean locked, Vector2 center)
    {
        this.room = room;
        this.center = new Vector2(center);
        this.roomEdge = roomEdge;
        this.fixture = fixture;
        this.locked = locked;
    }

    @Override
    public void update(float delta)
    {

    }

    /** @return false when the door is locked and stays closed */
    public boolean open()
    {
        if(locked)
            return false;
        closed = false;
        fixture.setSensor(true);
        return true;
    }

    public void unlock()
    {
        locked = false;
    }

    public void close()
    {
        closed = true;
        fixture.setSensor(false);
    }
}
