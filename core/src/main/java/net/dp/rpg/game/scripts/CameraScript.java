package net.dp.rpg.game.scripts;

import com.badlogic.gdx.graphics.OrthographicCamera;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.BoundingComponent;
import net.dp.rpg.engine.components.CameraComponent;
import net.dp.rpg.engine.components.FollowComponent;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.events.EnterRoomEvent;

public class CameraScript extends EntityScript implements GameEventListener
{
    private int target;

    public CameraScript(int target)
    {
        this.target = target;
    }

    public void update(float delta)
    {
        BoundingComponent bounds = getComponent(BoundingComponent.class);
        FollowComponent follow = getComponent(FollowComponent.class);
        if(bounds.isInside)
            follow.targetEntity = target;
        else
            follow.targetEntity = -1;
    }

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof EnterRoomEvent roomEvent)
        {
            OrthographicCamera camera = getComponent(CameraComponent.class).camera;

            TileRoom room = roomEvent.room;

            BoundingComponent bounds = getComponent(BoundingComponent.class);
            float minX = room.origin().x() * RoomGeometry.CELL_WIDTH;
            float minY = (7 - room.origin().y()) * RoomGeometry.CELL_HEIGHT - room.shape().tileHeight();
            bounds.set(minX, minY, minX + getScene().getTileMap().width(), minY + getScene().getTileMap().height(), camera.viewportWidth, camera.viewportHeight);
        }
    }
}
