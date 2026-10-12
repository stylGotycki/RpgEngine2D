package net.dp.rpg.game;

import lombok.Getter;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.floor.*;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.render.TileMapRenderer;
import net.dp.rpg.engine.tile.room.*;
import net.dp.rpg.game.level.EnterRoomEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TileFloor implements GameEventListener
{
    private static final String[] TILESET_PATHS = {"tiles/terrain.tsx", "tiles/basement.tsx"};
    private static final String MAP_PATH = "export/floor1/room-00.tmx";

    @Getter
    private final int gridWidth = 9;
    @Getter
    private final int gridHeight = 7;

    private final int roomCount;

    @Getter
    private final List<TileRoom> rooms = new ArrayList<>();

    private int activeRoom = 0;

    private boolean mapLoaded;

    private final FloorGenerator generator = new FloorGenerator();

    @Getter
    private TileSystem tileSystem;

    private List<String> tilesetIds;

    private TileMapRenderer renderer;

    private TileLogger logger;

    private FloorArchetype archetype = FloorArchetypes.CAVES;

    private FloorLayout layout;

    private TileMapData map;

    private Scene scene;

    private long seed;

    public TileFloor(long seed, int roomCount, Scene scene)
    {
        this.scene = scene;
        this.seed = seed;
        this.roomCount = roomCount;

        logger = new GdxTileLogger("Floor");

        tileSystem = new TileSystem(seed);
        tileSystem.loadTilesets(TILESET_PATHS);
        tilesetIds = tileSystem.tilesetIds();

        generate(seed);

        logger.log(tileSystem.debug().describeCatalog());

        scene.setTileMap(getActiveRoom().map(), getTileSystem(), getActiveRoom().origin().x(), getActiveRoom().origin().y(), this.gridHeight);
    }

    public TileRoom getActiveRoom()
    {
        return rooms.get(activeRoom);
    }

    public void nextRoomActive(Direction direction)
    {
        TileRoom currentRoom = rooms.get(activeRoom);
        RoomCell currentCell = currentRoom.origin();
        for(TileRoom room : rooms)
        {
            if(room.containsCell(currentCell.neighbour(direction)))
            {
                this.activeRoom = room.id();
                break;
            }
        }
        scene.setTileMap(getActiveRoom().map(), getTileSystem(), getActiveRoom().origin().x(), getActiveRoom().origin().y(), this.gridHeight);
    }

    public void setRoomActive(TileRoom room)
    {
        this.activeRoom = room.id();
        scene.setTileMap(getActiveRoom().map(), getTileSystem(), getActiveRoom().origin().x(), getActiveRoom().origin().y(), this.getGridHeight());
    }

    private void generate(long newSeed)
    {
        seed = newSeed;
        layout = generator.generate(archetype, seed);

        TileRoomPainter painter = new TileRoomPainter(tileSystem.types());

        new TileFloorGenerator(gridWidth, gridHeight).generate(seed, roomCount)
            .forEach(draft -> rooms.add(painter.paint(draft, seed)));

        logger.log("Generated floor: %d rooms, %d cells total".formatted(rooms.size(),
            rooms.stream().mapToInt(room -> room.shape().size()).sum()));

        for(TileRoom room : rooms)
        {
            logger.log("  %s at %s: %dx%d cells, %d door(s) -> %s".formatted(room.label(),
                room.origin(), room.shape().cellsAcross(), room.shape().cellsDown(),
                room.doors().size(), room.doors().stream().map(RoomEdge::direction).toList()));
        }
    }

    public void loadRoomFromFile() {
        if (mapLoaded) {
            logger.log("Map already loaded as room %d".formatted(rooms.size() - 1));

            return;
        }

        TileMapData map = tileSystem.loadMap(MAP_PATH);
        RoomShape shape;

        try {
            shape = TileRoomShapes.of(map);
        } catch (TileException exception) {
            logger.log("Cannot recover a room shape from %s, treating it as one cell: %s".formatted(MAP_PATH, exception.getMessage()));

            shape = RoomShape.single();
        }

        rooms.add(new TileRoom(rooms.size(), shape, RoomCell.ORIGIN, Set.of(), map));
        mapLoaded = true;

        logger.log("Loaded %s as room %d: %dx%d tiles, shape %s".formatted(MAP_PATH,
            rooms.size() - 1, map.width(), map.height(), TileRoomShapes.encode(shape)));

        activeRoom = (rooms.size() - 1);
    }

    @Override
    public void onEvent(GameEvent event)
    {
        if(event instanceof EnterRoomEvent doorEvent)
        {
            setRoomActive(doorEvent.room);
        }
    }
}
