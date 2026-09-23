package net.dp.rpg.engine.components;

import com.badlogic.gdx.math.MathUtils;

public class BoundingComponent implements Component
{
    public float minX;
    public float minY;
    public float maxX;
    public float maxY;

    // todo what if object changes its size?
    public BoundingComponent(float minX, float minY, float maxX, float maxY, float objectWidth, float objectHeight)
    {
        this.minX = minX + objectWidth/2;
        this.minY = minY + objectHeight/2;
        this.maxX = maxX - objectWidth/2;
        this.maxY = maxY - objectHeight/2;
    }
}
