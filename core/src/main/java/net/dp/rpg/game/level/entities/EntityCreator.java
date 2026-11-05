package net.dp.rpg.game.level.entities;

import net.dp.rpg.engine.Scene;

public abstract class EntityCreator
{
    protected final Scene scene;

    public EntityCreator(Scene scene)
    {
        this.scene = scene;
    }

    public abstract int create(float x, float y);
}
