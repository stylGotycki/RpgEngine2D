package net.dp.rpg.engine.systems;

import com.badlogic.gdx.physics.box2d.ChainShape;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.FloatArray;
import net.dp.rpg.engine.tile.*;
import net.dp.rpg.engine.tile.room.Direction;

import java.util.Arrays;
import java.util.Optional;

public class TileConverter
{
    private final float fieldSize = 1;
    private int tempWidth;
    private int tempHeight;
    TileTypeRegistry tempTypeRegistry;
    boolean[] visited;

    public Array<FixtureDef> creatFixtureDefsOnNotWalkable(TileMapData tileMap, TileSystem tileSystem)
    {
        Optional<TileLayer> optLayer = tileMap.findLayer(TileLayerKind.GROUND);
        if(optLayer.isEmpty())
            return null;

        tempTypeRegistry = tileSystem.types();
        TileLayer layer = optLayer.get();
        TileGrid grid = layer.grid();

        tempWidth = grid.getWidth();
        tempHeight = grid.getHeight();

        visited = new boolean[tempWidth*tempHeight];
        Arrays.fill(visited, false);

        Array<FixtureDef> fixtureDefs = new Array<>();

        for(int y = 0; y < tempHeight; y++)
        {
            for(int x = 0; x < tempWidth; x++)
            {
                if(visited[getArrayIndex(x,y)])
                    continue;

                if(!isWalkable(grid, x, y))
                {
                    fixtureDefs.add(createFixtureFrom(grid, x, y, Direction.EAST));
                    visited[getArrayIndex(x,y)] = true;
                }
                else if(!isWalkable(grid, x-1, y))
                {
                    fixtureDefs.add(createFixtureFrom(grid, x, y, Direction.NORTH));
                }
            }
        }

        return fixtureDefs;
    }

    private FixtureDef createFixtureFrom(TileGrid grid, int x, int y, Direction direction)
    {
        int firstX = x;
        int firxtY = y;
        FixtureDef fixtureDef = new FixtureDef();
        ChainShape shape = new ChainShape();

        FloatArray vertices = new FloatArray();

        addVertex(vertices, x, y); // start of the chain
        while(true)
        {
            switch (direction)
            {
                case NORTH -> y++;
                case EAST -> x++;
                case SOUTH -> y--;
                case WEST -> x--;
            }
            if((x == firstX && y == firxtY))
                break;
            switch (direction)
            {
                case NORTH ->
                {
                    if(!isWalkable(grid, x, y))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.EAST;
                    }
                    else if(isWalkable(grid, x-1, y))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.WEST;
                    }
                }
                case EAST ->
                {
                    if(!isWalkable(grid, x, y-1))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.SOUTH;
                    }
                    else if(isWalkable(grid, x, y))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.NORTH;
                    }
                }
                case SOUTH ->
                {
                    if(!isWalkable(grid, x-1, y-1))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.WEST;
                    }
                    else if(isWalkable(grid, x, y-1))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.EAST;
                    }
                }
                case WEST ->
                {
                    if(!isWalkable(grid, x-1, y))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.NORTH;
                    }
                    else if(isWalkable(grid, x-1, y-1))
                    {
                        addVertex(vertices, x, y);
                        direction = Direction.SOUTH;
                    }
                }
            }
        }

        if(vertices.size >= 6 && vertices.size % 2 == 0)
            shape.createLoop(vertices.items, 0, vertices.size);
        fixtureDef.shape = shape;
        return fixtureDef;
    }

    private void addVertex(FloatArray array, int x, int y)
    {
        array.add(x*fieldSize, (tempHeight-y)*fieldSize);
    }

    private boolean isWalkable(TileGrid grid, int x, int y)
    {
        if(x >= tempWidth || x < 0 || y >= tempHeight || y < 0)
            return true;
        visited[getArrayIndex(x,y)] = true;
        int runtimeId = grid.get(x, y);
        return runtimeId != TileGrid.EMPTY && tempTypeRegistry.require(runtimeId).walkable();
    }

    private int getArrayIndex(int x, int y)
    {
        return y * tempWidth + x;
    }
}
