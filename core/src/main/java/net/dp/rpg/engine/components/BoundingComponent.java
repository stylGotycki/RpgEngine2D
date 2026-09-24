package net.dp.rpg.engine.components;

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

    public BoundingComponent(float minX, float minY, float maxX, float maxY)
    {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
    }

    @Override
    public Component copy()
    {
        return new BoundingComponent(minX, minY, maxX, maxY);
    }
}
