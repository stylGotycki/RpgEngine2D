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

    @Getter
    private boolean locked;

    // Sealed by the game, not by the floor's graph: nothing opens it (no key, no debug K) until
    // the game unseals it. The boss room seals behind the player.
    @Getter
    private boolean sealed;
    private final Fixture fixture;

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

    public boolean open()
    {
        if(locked || sealed)
            return false;
        closed = false;
        fixture.setSensor(true);
        return true;
    }

    public void unlock()
    {
        locked = false;
    }

    // Closes the door for good until unseal(); a sealed door is always closed.
    public void seal()
    {
        sealed = true;
        close();
    }

    public void unseal()
    {
        sealed = false;
    }

    public void close()
    {
        closed = true;
        fixture.setSensor(false);
    }
}
