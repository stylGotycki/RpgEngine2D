package net.dp.rpg.engine;

import lombok.Getter;

public abstract class Script
{
    @Getter
    private Scene scene;

    @Getter
    private int entity;

    @Getter
    private EngineServices engine;

    final void setOwners(EngineServices engine, Scene scene, int entity)
    {
        this.engine = engine;
        this.scene = scene;
        this.entity = entity;
    }

    abstract public void update(float delta);
}
