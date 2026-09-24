package net.dp.rpg.engine.components;

public class FollowComponent implements Component
{
    public int targetEntity;
    public float lerp;

    public FollowComponent(int targetEntity, float lerp)
    {
        this.targetEntity = targetEntity;
        this.lerp = lerp;
    }

    @Override
    public Component copy()
    {
        return new FollowComponent(targetEntity, lerp);
    }
}
