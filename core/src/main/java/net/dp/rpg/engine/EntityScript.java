package net.dp.rpg.engine;

import lombok.Getter;

public abstract class EntityScript extends Script
{
    @Getter
    private Scene scene;

    @Getter
    private int entity;

    final void setOwners(EngineServices engine, Scene scene, int entity)
    {
        this.engine = engine;
        this.scene = scene;
        this.entity = entity;
    }
}
