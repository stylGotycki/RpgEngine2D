package net.dp.rpg.engine.systems;

import com.badlogic.gdx.math.Vector2;
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
    private float fieldSize = 1;
    private int tempWidth;
    private int tempHeight;
    TileTypeRegistry tempTypeRegistry;
    private boolean[] visited;

    public void setTileMapParams(float fieldSize, int width, int height)
    {
        this.fieldSize = fieldSize;
        this.tempWidth = width;
        this.tempHeight = height;
    }

    public float convertToWorldY(float y)
    {
        return (tempHeight-y)*fieldSize;
    }

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

        visited = new boolean[tempWidth*tempHeight*4];
        Arrays.fill(visited, false);

        Array<FixtureDef> fixtureDefs = new Array<>();

        for(int y = 0; y < tempHeight; y++)
            for(int x = 0; x < tempWidth; x++)
            {
                if(!isWalkable(grid,x,y))
                    for(Direction dir : Direction.values())
                    {
                        if(!visited[getArrayIndex(x,y,dir)] && isWalkable(grid, x+dir.getDeltaX(), y-dir.getDeltaY()))
                        {
                            FixtureDef newFixture = createFixtureFrom(grid, x, y, dir);
                            if(newFixture != null)
                                fixtureDefs.add(newFixture);
                            setVisited(x,y,dir);
                        }
                    }
            }

        return fixtureDefs;
    }

    public static void convertToWorldCoords(Vector2 coords, float fieldSize, int tilemapHeight)
    {
        coords.set(coords.x*fieldSize, (tilemapHeight-coords.y)*fieldSize);
    }

    private FixtureDef createFixtureFrom(TileGrid grid, int x, int y, Direction direction)
    {
        FixtureDef fixtureDef = new FixtureDef();
        ChainShape shape = new ChainShape();

        FloatArray vertices = new FloatArray();

        // set initial direction of drawing the chain
        switch (direction)
        {
            case NORTH ->
                {
                    direction = Direction.WEST;
                    y++;
                    x++;
                }
            case EAST ->
                {
                    direction = Direction.NORTH;
                    x++;
                }
            case SOUTH ->
                {
                    direction = Direction.EAST;
                }
            case WEST ->
                {
                    direction = Direction.SOUTH;
                    y++;
                }
        }

        int firstX = x;
        int firxtY = y;

        // start of the chain
        addVertex(vertices, x, y);
        while(true)
        {
            // set visited edge
            switch(direction)
            {
                case NORTH -> setVisited(x-1,y,Direction.EAST);
                case EAST -> setVisited(x,y,Direction.SOUTH);
                case SOUTH -> setVisited(x,y-1,Direction.WEST);
                case WEST -> setVisited(x-1,y-1,Direction.NORTH);
            }

            // move cursor
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
        {
            shape.createLoop(vertices.items, 0, vertices.size);
            fixtureDef.shape = shape;
            return fixtureDef;
        }
        return null;
    }

    private void addVertex(FloatArray array, int x, int y)
    {
        array.add(x*fieldSize, (tempHeight-y)*fieldSize);
    }

    private boolean isWalkable(TileGrid grid, int x, int y)
    {
        if(x >= tempWidth || x < 0 || y >= tempHeight || y < 0)
            return true;
        int runtimeId = grid.get(x, y);
        if(runtimeId == TileGrid.EMPTY)
            return true;
        return tempTypeRegistry.require(runtimeId).walkable();
    }

    private int getArrayIndex(int x, int y, Direction dir)
    {
        return ( y * tempWidth + x ) * 4 + dir.ordinal();
    }

    private void convertToWorldCoords(Vector2 coords)
    {
        coords.set(coords.x*fieldSize, (tempHeight-coords.y)*fieldSize);
    }

    private void setVisited(int x, int y, Direction dir)
    {
        visited[getArrayIndex(x,y,dir)] = true;
    }
}
