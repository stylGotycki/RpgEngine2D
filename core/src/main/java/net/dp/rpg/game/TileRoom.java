package net.dp.rpg.game;

import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.room.RoomCell;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomShape;

import java.util.Set;

public record TileRoom(int id, RoomShape shape, RoomCell origin, Set<RoomEdge> doors, TileMapData map) {

    public String label() {
        return "room-%02d".formatted(id);
    }

    public boolean containsCell(RoomCell cell)
    {
        for(RoomCell roomCell : shape.cells())
        {
            if(origin.x() + roomCell.x() == cell.x() && origin.y() + roomCell.y() == cell.y())
                return true;
        }
        return false;
    }

    public RoomCell toFloorCell(RoomCell localCell) {
        return localCell.translated(origin.x(), origin.y());
    }
}
