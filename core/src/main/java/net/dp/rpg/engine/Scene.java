package net.dp.rpg.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import lombok.Getter;
import lombok.Setter;
import net.dp.rpg.engine.bodyCreator.PhysicalBodyCreator;
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
        componentStorages.add(new ComponentStorage<>(PhysicalBodyComponent.class));
        componentStorages.add(new ComponentStorage<>(SpriteComponent.class));
    }

    public <T extends Component> void addComponent(int entity, T component)
    {
        getComponentStorage((Class<T>) component.getClass()).add(entity, component);
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

    //todo delete this func once PhysicalBodyCreator has full functionality
    public Body addComponent(int entity, BodyDef bodyDef)
    {
        Body body = physicalWorld.createBody(bodyDef);
        addComponent(entity, new PhysicalBodyComponent(body));
        return body;
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
        //todo make less physics steps on higher refresh rate (fix to const refresh rate)
        physicalWorld.step(delta, 6, 2);

        movementSystem.updatePositions(getComponentStorage(TransformComponent.class), getComponentStorage(MoveComponent.class), delta);
        movementSystem.syncBodyAndSpritePositions(getComponentStorage(PhysicalBodyComponent.class), getComponentStorage(SpriteComponent.class));

    }

    public void debugRender()
    {
        debugRenderer.render(physicalWorld, camera.combined);
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
        debugRenderer.dispose();
    }

    private int nextEntity = 0;
    private final World physicalWorld = new World(new Vector2(0,0), true);

    @Setter
    @Getter
    private OrthographicCamera camera = new OrthographicCamera(20, (float) Gdx.graphics.getHeight()/Gdx.graphics.getWidth() * 20);

    @Getter
    private final PhysicalBodyCreator bodyCreator = new PhysicalBodyCreator();

    private final Box2DDebugRenderer debugRenderer = new Box2DDebugRenderer();

    private final ArrayList<Integer> freeEntity = new ArrayList<>();
    private final ArrayList<ComponentStorage<? extends Component>> componentStorages = new ArrayList<>();

    private final MovementSystem movementSystem = new MovementSystem();
}
