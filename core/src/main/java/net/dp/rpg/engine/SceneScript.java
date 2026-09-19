package net.dp.rpg.engine;

import lombok.Getter;

public abstract class SceneScript extends Script
{
    @Getter
    Scene scene;

    final void setOwners(EngineServices engine, Scene scene)
    {
        this.engine = engine;
        this.scene = scene;
    }
}
