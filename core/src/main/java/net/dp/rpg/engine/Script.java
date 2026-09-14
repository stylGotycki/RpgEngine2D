package net.dp.rpg.engine;

import lombok.Getter;

public abstract class Script
{
    @Getter
    private Scene scene;

    @Getter
    private int entity;

    final void setOwners(Scene scene, int entity)
    {
        this.scene = scene;
        this.entity = entity;
    }

    abstract public void update(float delta);
}
