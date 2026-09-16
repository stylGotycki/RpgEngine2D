package net.dp.rpg.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import lombok.Getter;
import lombok.Setter;
import net.dp.rpg.engine.bodyCreator.PhysicalBodyCreator;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.components.Component;
import net.dp.rpg.engine.systems.MovementSystem;

import java.util.ArrayList;

public class Scene
{
    private final EngineServices engine;
    @Getter
    private final World physicalWorld = new World(new Vector2(0, 0), true);

    @Getter
    private final Gui gui = new Gui();

    @Setter
    @Getter
    private OrthographicCamera camera = new OrthographicCamera(20, (float) Gdx.graphics.getHeight() / Gdx.graphics.getWidth() * 20);

    @Getter
    private final PhysicalBodyCreator bodyCreator = new PhysicalBodyCreator();

    private int nextEntity = 0;
    private final ArrayList<Integer> freeEntity = new ArrayList<>();
    private final ArrayList<ComponentStorage<? extends Component>> componentStorages = new ArrayList<>();

    private final MovementSystem movementSystem = new MovementSystem();

    Scene(EngineServices engine)
    {
        this.engine = engine;

        componentStorages.add(new ComponentStorage<>(MoveComponent.class));
        componentStorages.add(new ComponentStorage<>(TransformComponent.class));
        componentStorages.add(new ComponentStorage<>(TextureComponent.class));
        componentStorages.add(new ComponentStorage<>(PhysicalBodyComponent.class));
        componentStorages.add(new ComponentStorage<>(SpriteComponent.class));
        componentStorages.add(new ComponentStorage<>(ScriptComponent.class));
    }

    public void resize(int width, int height)
    {
        gui.resize(width, height);
    }

    public <T extends Component> void addComponent(int entity, T component)
    {
        getComponentStorage((Class<T>) component.getClass()).add(entity, component);
        if (component instanceof ScriptComponent scriptComp)
        {
            scriptComp.script.setOwners(engine, this, entity);
        }
    }

    /**
     * creates Body using parameters set in PhysicalBodyCreator and adds it as Component
     */
    public Body createBodyComponent(int entity)
    {
        Body body = physicalWorld.createBody(bodyCreator.getBodyDef());
        addComponent(entity, new PhysicalBodyComponent(body));
        return body;
    }

    public Fixture createFixture(int entity)
    {
        Body body = getComponentStorage(PhysicalBodyComponent.class).getByEntity(entity).body;
        return body.createFixture(bodyCreator.getFixtureDef());
    }

    public <T extends Component> ComponentStorage<T> getComponentStorage(Class<T> componentClass)
    {
        for (ComponentStorage<? extends Component> componentStorage : componentStorages)
        {
            if (componentStorage.getComponentClass() == componentClass)
                return (ComponentStorage<T>) componentStorage;
        }
        return null;
    }

    public void update(float delta)
    {
        ComponentStorage<ScriptComponent> scriptsStorage = getComponentStorage(ScriptComponent.class);
        for (int i = 0; i < scriptsStorage.size(); i++)
        {
            scriptsStorage.getByIndex(i).script.update(delta);
        }

        //todo make less physics steps on higher refresh rate (fix to const refresh rate)
        physicalWorld.step(delta, 6, 2);

        movementSystem.updatePositions(getComponentStorage(TransformComponent.class), getComponentStorage(MoveComponent.class), delta);
        movementSystem.syncBodyAndSpritePositions(getComponentStorage(PhysicalBodyComponent.class), getComponentStorage(SpriteComponent.class));

        gui.update(delta);
    }

    public int createEntity()
    {
        if(!freeEntity.isEmpty())
            return freeEntity.getFirst();

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

    public void dispose()
    {
        gui.dispose();
    }
}
