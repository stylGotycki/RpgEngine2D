package net.dp.rpg.engine;

import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.systems.MovementSystem;

import java.util.ArrayList;

public class Scene
{
    public Scene()
    {
        componentStorages.add(new ComponentStorage<>(MoveComponent.class));
        componentStorages.add(new ComponentStorage<>(TransformComponent.class));
        componentStorages.add(new ComponentStorage<>(TextureComponent.class));
    }

    public <T extends Component> void addComponent(int entity, T component)
    {
        getComponentStorage((Class<T>) component.getClass()).add(entity, component);
    }

    public <T extends Component> ComponentStorage<T> getComponentStorage(Class<T> componentClass)
    {
        for(ComponentStorage<? extends Component> componentStorage : componentStorages)
        {
            if(componentStorage.getComponentClass() == componentClass)
                return (ComponentStorage<T>) componentStorage;
        }
        return null;
    }

    public void update(float delta)
    {
        movementSystem.updatePositions(getComponentStorage(TransformComponent.class), getComponentStorage(MoveComponent.class), delta);
    }

    public int createEntity()
    {
        return nextEntity++;
    }

    public void deleteEntity(int entityId)
    {
        freeEntity.add(entityId);
        for(ComponentStorage<? extends Component> componentStorage : componentStorages)
        {
            componentStorage.remove(entityId);
        }
    }

    private int nextEntity = 0;
    private final ArrayList<Integer> freeEntity = new ArrayList<>();
    private final ArrayList<ComponentStorage<? extends Component>> componentStorages = new ArrayList<>();

    private final MovementSystem movementSystem = new MovementSystem();
}
