package net.dp.rpg.engine.components;

public class BoundingComponent implements Component
{
    public float minX;
    public float minY;
    public float maxX;
    public float maxY;

    public boolean smoothCorrection = true;
    public float lerp = 8f;

    // todo what if object changes its size?
    public BoundingComponent(float minX, float minY, float maxX, float maxY, float objectWidth, float objectHeight)
    {
        set(minX + objectWidth/2, minY + objectHeight/2, maxX - objectWidth/2, maxY - objectHeight/2);
    }

    public BoundingComponent(float minX, float minY, float maxX, float maxY)
    {
        set(minX,minY,maxX,maxY);
    }

    public void set(float minX, float minY, float maxX, float maxY)
    {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;

        if(minX > maxX)
        {
            this.minX = this.maxX = (minX+maxX)/2;
        }
        if(minY > maxY)
        {
            this.minY = this.maxY = (minY+maxY)/2;
        }
    }

    public void set(float minX, float minY, float maxX, float maxY, float objectWidth, float objectHeight)
    {
        set(minX + objectWidth/2, minY + objectHeight/2, maxX - objectWidth/2, maxY - objectHeight/2);
    }

    @Override
    public Component copy()
    {
        return new BoundingComponent(minX, minY, maxX, maxY);
    }
}
