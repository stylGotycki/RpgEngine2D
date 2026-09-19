package net.dp.rpg.engine;

import lombok.Getter;

public abstract class Script
{
    @Getter
    EngineServices engine;

    final void setOwners(EngineServices engine)
    {
        this.engine = engine;
    }

    abstract public void update(float delta);
}
