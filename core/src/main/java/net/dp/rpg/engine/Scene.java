package net.dp.rpg.engine;

import box2dLight.PointLight;
import box2dLight.RayHandler;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Disposable;
import lombok.Getter;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.components.Component;
import net.dp.rpg.engine.systems.AnimationSystem;
import net.dp.rpg.engine.systems.CollisionDispatcher;
import net.dp.rpg.engine.systems.MovementSystem;
import net.dp.rpg.engine.systems.input.EngineInputAdapter;
import net.dp.rpg.engine.systems.input.InputListener;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.room.RoomGeometry;

import java.util.ArrayList;

public class Scene implements Disposable
{
    private final EngineServices engine;
    @Getter
    private final World physicalWorld = new World(new Vector2(0, 0), true);
    CollisionDispatcher collisionDispatcher = new CollisionDispatcher(this);

    @Getter
    private TileSystem tileSystem;
    @Getter
    private TileMapData tileMap;
    @Getter
    private float tileTransformX;
    @Getter
    private float tileTransformY;

    @Getter
    private final Gui gui = new Gui();

    @Getter
    private OrthographicCamera activeCamera = new OrthographicCamera(20, (float) Gdx.graphics.getHeight() / Gdx.graphics.getWidth() * 20);

    private int nextEntity = 0;
    private final ArrayList<Integer> freeEntity = new ArrayList<>();
    private final ArrayList<ComponentStorage<? extends Component>> componentStorages = new ArrayList<>();
    private final ArrayList<Integer> toDeleteEntities = new ArrayList<>();

    private SceneScript sceneScript;

    private final EngineInputAdapter inputAdapter;

    private final MovementSystem movementSystem = new MovementSystem();
    private final AnimationSystem animationSystem = new AnimationSystem();

    @Getter
    private final RayHandler rayHandler = new RayHandler(physicalWorld);

    Scene(EngineServices engine, EngineInputAdapter inputAdapter)
    {
        this.engine = engine;
        this.inputAdapter = inputAdapter;

        physicalWorld.setContactListener(collisionDispatcher);

        componentStorages.add(new ComponentStorage<>(MoveComponent.class));
        componentStorages.add(new ComponentStorage<>(TransformComponent.class));
        componentStorages.add(new ComponentStorage<>(PhysicalBodyComponent.class));
        componentStorages.add(new ComponentStorage<>(MultiSpriteComponent.class));
        componentStorages.add(new ComponentStorage<>(ScriptComponent.class));
        componentStorages.add(new ComponentStorage<>(CameraComponent.class));
        componentStorages.add(new ComponentStorage<>(FollowComponent.class));
        componentStorages.add(new ComponentStorage<>(BoundingComponent.class));
        componentStorages.add(new ComponentStorage<>(HealthComponent.class));
        componentStorages.add(new ComponentStorage<>(LightComponent.class));
        componentStorages.add(new ComponentStorage<>(MultiAnimationComponent.class));
        componentStorages.add(new ComponentStorage<>(SpriteComponent.class));
        componentStorages.add(new ComponentStorage<>(AnimationComponent.class));

        RayHandler.useDiffuseLight(true);
    }

    public <T extends Component> void addComponent(int entity, T component)
    {
        if(component instanceof MultiAnimationComponent && (getComponentStorage(AnimationComponent.class).hasComponent(entity) || getComponentStorage(SpriteComponent.class).hasComponent(entity)))
        {
            throw new IllegalStateException("Cannot add MultiAnimationComponent: entity already has AnimationComponent or SpriteComponent.");
        }
        else if(component instanceof AnimationComponent && (getComponentStorage(MultiAnimationComponent.class).hasComponent(entity) || getComponentStorage(MultiSpriteComponent.class).hasComponent(entity)))
        {
            throw new IllegalStateException("Cannot add AnimationComponent: entity already has MultiAnimationComponent or MultiSpriteComponent.");
        }
        else if(component instanceof MultiSpriteComponent && getComponentStorage(SpriteComponent.class).hasComponent(entity))
        {
            throw new IllegalStateException("Cannot add MultiSpriteComponent: entity already has SpriteComponent.");
        }
        else if(component instanceof SpriteComponent && getComponentStorage(MultiSpriteComponent.class).hasComponent(entity))
        {
            throw new IllegalStateException("Cannot add SpriteComponent: entity already has MultiSpriteComponent.");
        }
        getComponentStorage((Class<T>) component.getClass()).add(entity, component);
        if (component instanceof ScriptComponent scriptComp)
        {
            scriptComp.script.setOwners(engine, this, entity);
            if(scriptComp.script instanceof InputListener)
                inputAdapter.addListener((InputListener) scriptComp.script);
        }
        else if ((component instanceof MultiSpriteComponent
            || component instanceof SpriteComponent
            || component instanceof CameraComponent
            || component instanceof LightComponent)
            && !getComponentStorage(TransformComponent.class).hasComponent(entity))
        {
            addComponent(entity, new TransformComponent(new Vector2(0,0), 0, 1));
        }
        else if(component instanceof PhysicalBodyComponent bodyComponent && !getComponentStorage(TransformComponent.class).hasComponent(entity))
        {
            addComponent(entity, new TransformComponent(new Vector2(bodyComponent.body.getPosition().x,bodyComponent.body.getPosition().y), 0, 1));
        }
        else if(component instanceof MultiAnimationComponent animationComponent && !getComponentStorage(MultiSpriteComponent.class).hasComponent(entity))
        {
            MultiSpriteComponent spriteComponent = new MultiSpriteComponent();
            spriteComponent.add(new Sprite(), false);
            if(animationComponent.animationSets.size > 0)
                spriteComponent.sprites.get(0).setRegion(animationComponent.animationSets.get(0).getKeyFrame(0));
            addComponent(entity, spriteComponent);
        }
        else if(component instanceof AnimationComponent animationComponent && !getComponentStorage(SpriteComponent.class).hasComponent(entity))
        {
            SpriteComponent spriteComponent = new SpriteComponent(new Sprite(), false);
            spriteComponent.sprite.setRegion(animationComponent.animationSet.getKeyFrame(0));
            addComponent(entity, spriteComponent);
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
     * creates Body using parameters set in bodyDef and adds it as Component
     */
    public Body createBodyComponent(int entity, BodyDef bodyDef)
    {
        Body body = physicalWorld.createBody(bodyDef);
        body.setUserData(entity);
        addComponent(entity, new PhysicalBodyComponent(body));
        return body;
    }

    public PointLight createPointLightComponent(int entity, int rays, Color color, float distance, boolean xRay)
    {
        PointLight light = new PointLight(rayHandler, rays, color, distance, 0, 0);
        if(getComponentStorage(PhysicalBodyComponent.class).hasComponent(entity))
        {
            Body body = getComponentStorage(PhysicalBodyComponent.class).getByEntity(entity).body;
            light.attachToBody(body);
            light.setPosition(body.getPosition());
        }
        else if(getComponentStorage(TransformComponent.class).hasComponent(entity))
        {
            light.setPosition(getComponentStorage(TransformComponent.class).getByEntity(entity).position);
        }
        addComponent(entity, new LightComponent(light));
        light.setXray(xRay);
        light.setSoftnessLength(4f);
        return light;
    }

    public Fixture createFixture(int entity, FixtureDef fixtureDef)
    {
        Body body = getComponentStorage(PhysicalBodyComponent.class).getByEntity(entity).body;
        return body.createFixture(fixtureDef);
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

    public void setToDelete(int entityId)
    {
        toDeleteEntities.add(entityId);
    }

    private void deleteEntities()
    {
        for(int entity : toDeleteEntities)
        {
            deleteEntity(entity);
        }
        toDeleteEntities.clear();
    }

    private void deleteEntity(int entityId)
    {
        freeEntity.add(entityId);
        for(ComponentStorage<? extends Component> componentStorage : componentStorages)
        {
            if(componentStorage.getComponentClass() == PhysicalBodyComponent.class && componentStorage.hasComponent(entityId))
            {
                Body body = ((PhysicalBodyComponent)componentStorage.getByEntity(entityId)).body;
                physicalWorld.destroyBody(body);
            }
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
        CameraComponent component = getComponentStorage(CameraComponent.class).getByEntity(entityId);
        if(component != null)
            this.activeCamera = component.camera;
    }

    public void setTileMap(TileMapData tileMap, TileSystem tileSystem, float xPos, float yPos, int gridHeight)
    {
        if(tileMap != null && tileSystem != null)
        {
            this.tileSystem = tileSystem;
            this.tileMap = tileMap;
            this.tileTransformX = xPos * RoomGeometry.CELL_WIDTH;
            this.tileTransformY = (gridHeight - yPos) * RoomGeometry.CELL_HEIGHT - tileMap.height();
        }
    }

    public void setAmbientLight(Color light)
    {
        rayHandler.setAmbientLight(light);
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

        movementSystem.transformToBody(getComponentStorage(PhysicalBodyComponent.class), getComponentStorage(TransformComponent.class));
        //todo make less physics steps on higher refresh rate (fix to const refresh rate)
        physicalWorld.step(delta, 6, 2);

        movementSystem.updatePositions(getComponentStorage(TransformComponent.class), getComponentStorage(MoveComponent.class), delta);
        movementSystem.bodyToTransform(getComponentStorage(PhysicalBodyComponent.class), getComponentStorage(TransformComponent.class));
        movementSystem.follow(getComponentStorage(FollowComponent.class), getComponentStorage(TransformComponent.class), getComponentStorage(BoundingComponent.class), delta);
        movementSystem.boundPositions(getComponentStorage(BoundingComponent.class), getComponentStorage(TransformComponent.class), delta);
        movementSystem.transformToSprite(getComponentStorage(TransformComponent.class), getComponentStorage(MultiSpriteComponent.class), getComponentStorage(SpriteComponent.class));
        movementSystem.transformToCamera(getComponentStorage(TransformComponent.class), getComponentStorage(CameraComponent.class));
        movementSystem.transformToLight(getComponentStorage(TransformComponent.class), getComponentStorage(LightComponent.class));

        animationSystem.updateAnimations(getComponentStorage(SpriteComponent.class), getComponentStorage(AnimationComponent.class), delta);
        animationSystem.updateMultiAnimations(getComponentStorage(MultiSpriteComponent.class), getComponentStorage(MultiAnimationComponent.class), delta);

        gui.update(delta);

        deleteEntities();
    }

    public void resize(int width, int height)
    {
        gui.resize(width, height);
        activeCamera.viewportWidth = (float) width / height * activeCamera.viewportHeight;
        activeCamera.update();
    }

    public void dispose()
    {
        if(sceneScript != null && sceneScript instanceof InputListener listener)
        {
            inputAdapter.removeListener(listener);
        }
        ComponentStorage<ScriptComponent> scriptComponents = getComponentStorage(ScriptComponent.class);
        for(int i = 0; i < scriptComponents.size(); i++)
        {
            ScriptComponent component = scriptComponents.getByIndex(i);
            if(component.script instanceof InputListener listener)
            {
                inputAdapter.removeListener(listener);
            }
        }
        rayHandler.dispose();
        gui.dispose();
    }
}
