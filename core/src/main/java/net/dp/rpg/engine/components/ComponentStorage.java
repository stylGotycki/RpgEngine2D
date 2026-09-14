package net.dp.rpg.engine.components;

import lombok.Getter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ComponentStorage<T extends Component>
{
    @Getter
    private final Class<T> componentClass;
    private final ArrayList<T> components = new ArrayList<>();
    private final ArrayList<Integer> entities = new ArrayList<>();
    private final Map<Integer, Integer> entityToComponentIndex = new HashMap<>();

    public ComponentStorage(Class<T> componentClass)
    {
        this.componentClass = componentClass;
    }

    public int size()
    {
        return components.size();
    }

    public T getByIndex(int index)
    {
        if(components.size() > index)
            return components.get(index);
        else
            return null;
    }

    public T getByEntity(int entityId)
    {
        Integer index = entityToComponentIndex.get(entityId);
        return index == null ? null : components.get(index);
    }

    public int getEntity(int index)
    {
        return entities.get(index);
    }

    public boolean hasComponent(int entityId)
    {
        return entityToComponentIndex.containsKey(entityId);
    }

    public void add(int entityId, T component)
    {
        if(hasComponent(entityId))
            throw new IllegalArgumentException("Entity already has this component");

        components.add(component);
        entities.add(entityId);
        entityToComponentIndex.put(entityId, components.size()-1);
    }

    public void remove(int entityId)
    {
        Integer index = entityToComponentIndex.get(entityId);

        if(index == null)
            return;

        int last = components.size()-1;

        if(last != index)
        {
            int lastEntity = entities.getLast();
            components.set(index, components.get(last));
            entities.set(index, lastEntity);
            entityToComponentIndex.replace(lastEntity, index);
        }
        entityToComponentIndex.remove(entityId);
        components.remove(last);
        entities.remove(last);
    }
}
