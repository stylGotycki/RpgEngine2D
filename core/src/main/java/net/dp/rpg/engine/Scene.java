package net.dp.rpg.engine;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import lombok.Getter;
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

    @Getter
    private OrthographicCamera activeCamera = new OrthographicCamera(20, (float) Gdx.graphics.getHeight() / Gdx.graphics.getWidth() * 20);

    @Getter
    private final PhysicalBodyCreator bodyCreator = new PhysicalBodyCreator();

    private int nextEntity = 0;
    private final ArrayList<Integer> freeEntity = new ArrayList<>();
    private final ArrayList<ComponentStorage<? extends Component>> componentStorages = new ArrayList<>();

    private SceneScript sceneScript;

    private final EngineInputAdapter inputAdapter;

    private final MovementSystem movementSystem = new MovementSystem();

    Scene(EngineServices engine, EngineInputAdapter inputAdapter)
    {
        this.engine = engine;
        this.inputAdapter = inputAdapter;

        componentStorages.add(new ComponentStorage<>(MoveComponent.class));
        componentStorages.add(new ComponentStorage<>(TransformComponent.class));
        componentStorages.add(new ComponentStorage<>(TextureComponent.class));
        componentStorages.add(new ComponentStorage<>(PhysicalBodyComponent.class));
        componentStorages.add(new ComponentStorage<>(SpriteComponent.class));
        componentStorages.add(new ComponentStorage<>(ScriptComponent.class));
        componentStorages.add(new ComponentStorage<>(CameraComponent.class));
    }

    public <T extends Component> void addComponent(int entity, T component)
    {
        getComponentStorage((Class<T>) component.getClass()).add(entity, component);
        if (component instanceof ScriptComponent scriptComp)
        {
            scriptComp.script.setOwners(engine, this, entity);
            if(scriptComp.script instanceof InputListener)
                inputAdapter.addListener((InputListener) scriptComp.script);
        }
    }

    public <T extends Component> void removeComponent(int entity, T component)
    {
        getComponentStorage((Class<T>) component.getClass()).remove(entity);
        if (component instanceof ScriptComponent scriptComp && scriptComp.script instanceof InputListener listener)
        {
            inputAdapter.removeListener(listener);
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

    public void setSceneScript(SceneScript script)
    {
        if(sceneScript != null && sceneScript instanceof InputListener listener)
            inputAdapter.removeListener(listener);
        if(script instanceof InputListener listener)
            inputAdapter.addListener(listener);
        script.setOwners(engine, this);
        sceneScript = script;
    }

    public void setActiveCamera(int entityId)
    {
        CameraComponent component = getComponentStorage(CameraComponent.class).getByIndex(entityId);
        if(component != null)
            this.activeCamera = component.camera;
    }

    public void update(float delta)
    {
        if(sceneScript != null)
        {
            sceneScript.update(delta);
        }

        ComponentStorage<ScriptComponent> scriptsStorage = getComponentStorage(ScriptComponent.class);
        for (int i = 0; i < scriptsStorage.size(); i++)
        {
            scriptsStorage.getByIndex(i).script.update(delta);
        }

        //todo make less physics steps on higher refresh rate (fix to const refresh rate)
        physicalWorld.step(delta, 6, 2);

        movementSystem.updatePositions(getComponentStorage(TransformComponent.class), getComponentStorage(MoveComponent.class), delta);
        movementSystem.syncBodyAndSpritePositions(getComponentStorage(PhysicalBodyComponent.class), getComponentStorage(SpriteComponent.class));
        movementSystem.updateCameraPosition(getComponentStorage(CameraComponent.class), getComponentStorage(PhysicalBodyComponent.class), getComponentStorage(TransformComponent.class));

        gui.update(delta);
    }

    public void resize(int width, int height)
    {
        gui.resize(width, height);
        activeCamera.viewportWidth = (float) width / height * activeCamera.viewportHeight;
        activeCamera.update();
    }

    public void dispose()
    {
        gui.dispose();
    }
}
