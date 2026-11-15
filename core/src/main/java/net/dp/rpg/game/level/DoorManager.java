package net.dp.rpg.game.level;

import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.IntMap;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.level.events.EnemyKilledEvent;
import net.dp.rpg.game.level.events.EnterRoomEvent;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.scripts.DoorScript;

public class DoorManager implements GameEventListener
{
    public static final String SEALED_MESSAGE = "Nie ma odwrotu!";

    private final IntMap<Array<DoorScript>> roomDoors = new IntMap<>();
    private final TileFloor floor;
    private final BossDefeated bossDefeated;
    private boolean bossDead;

    // Told once, when the boss of this floor dies.
    public interface BossDefeated
    {
        void onBossDefeated(TileRoom room);
    }

    public DoorManager(TileFloor floor)
    {
        this(floor, room -> {});
    }

    public DoorManager(TileFloor floor, BossDefeated bossDefeated)
    {
        this.floor = floor;
        this.bossDefeated = bossDefeated;
    }

    public void addDoor(int roomId, DoorScript door)
    {
        if(!roomDoors.containsKey(roomId))
            roomDoors.put(roomId, new Array<>());
        roomDoors.get(roomId).add(door);
    }

    @Override
    public void onEvent(GameEvent event)
    {
        // The event bus outlives scenes and room ids repeat between floors, so only own rooms count.
        if(event instanceof EnterRoomEvent roomEvent && floor.owns(roomEvent.room))
        {
            Array<DoorScript> doors = roomDoors.get(roomEvent.room.id());
            if(doors == null)
                return;
            // After the boss is dead the room is an ordinary one again.
            boolean bossRoom = roomEvent.room.node().type() == RoomType.BOSS && !bossDead;
            boolean alreadySealed = doors.size > 0 && doors.first().isSealed();
            for(DoorScript door : doors)
            {
                if(bossRoom)
                    door.seal();
                else
                    door.close();
            }
            if(bossRoom && !alreadySealed)
                message(SEALED_MESSAGE);
        }
        else if(event instanceof EnemyKilledEvent killed && floor.owns(killed.room)
                && killed.enemy != null && ContentTags.BOSS.equals(killed.enemy.id()))
        {
            if(bossDead)
                return;
            bossDead = true;
            Array<DoorScript> doors = roomDoors.get(killed.room.id());
            if(doors != null)
                for(DoorScript door : doors)
                    door.unseal();
            bossDefeated.onBossDefeated(killed.room);
        }
    }

    private static void message(String text)
    {
        Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = text);
    }
}
