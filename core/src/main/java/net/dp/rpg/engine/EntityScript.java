package net.dp.rpg.engine;

import lombok.Getter;
import net.dp.rpg.engine.components.Component;

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

    public void onCollision(int otherEntity, short category, short otherCategory)
    {
    }

    public void onEndCollision(int otherEntity, short category, short otherCategory)
    {
    }

    protected final <T extends Component> T getComponent(Class<T> componentClass)
    {
        return scene.getComponentStorage(componentClass).getByEntity(entity);
    }
}
