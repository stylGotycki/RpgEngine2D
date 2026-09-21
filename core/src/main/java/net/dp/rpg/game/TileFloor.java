package net.dp.rpg.game;

import lombok.Getter;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.debug.GdxTileLogger;
import net.dp.rpg.engine.tile.debug.TileLogger;
import net.dp.rpg.engine.tile.exception.TileException;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class TileFloor
{
    private static final String[] TILESET_PATHS = {"tiles/terrain.tsx", "tiles/basement.tsx"};
    private static final String MAP_PATH = "export/floor1/room-01.tmx";

    private final long seed;
    private final int roomCount;

    private final List<TileRoom> rooms = new ArrayList<>();

    @Getter
    private TileSystem tileSystem;

    private List<String> tilesetIds;

    private TileLogger logger;

    private int activeRoom;

    private int activeTileset;

    private boolean mapLoaded;

    public TileFloor(long seed, int roomCount)
    {
        this.seed = seed;
        this.roomCount = roomCount;

        logger = new GdxTileLogger("Floor");

        tileSystem = new TileSystem(seed);
        tileSystem.loadTilesets(TILESET_PATHS);
        tilesetIds = tileSystem.tilesetIds();

        generateFloor();

        logger.log(tileSystem.debug().describeCatalog());

        activeRoom = 0;
    }

    public TileRoom getActiveRoom()
    {
        return rooms.get(activeRoom);
    }

    public void nextRoomActive()
    {
        activeRoom = (activeRoom + 1) % rooms.size();
    }

    public void previousRoomActive()
    {
        activeRoom = (activeRoom - 1 + rooms.size()) % rooms.size();
    }

    private void generateFloor()
    {
        TileRoomPainter painter = new TileRoomPainter(tileSystem.types());

        new TileFloorGenerator(9, 7).generate(seed, roomCount)
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
}
