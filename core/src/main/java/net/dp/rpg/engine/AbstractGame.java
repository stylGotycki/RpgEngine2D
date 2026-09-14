package net.dp.rpg.engine;

import lombok.Getter;

public abstract class AbstractGame
{
    @Getter
    private EngineServices engine;

    void setEngine(EngineServices engine)
    {
        this.engine = engine;
    }

    public abstract void create();

    public void setActiveScene(Scene scene)
    {
        engine.setActiveScene(scene);
    }
}
