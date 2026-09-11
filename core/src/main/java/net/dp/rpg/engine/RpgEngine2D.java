package net.dp.rpg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import net.dp.rpg.engine.bodyCreator.BodyParams;
import net.dp.rpg.engine.bodyCreator.FixtureParams;
import net.dp.rpg.engine.components.PhysicalBodyComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.engine.systems.RenderSystem;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class RpgEngine2D extends ApplicationAdapter
{
    @Override
    public void create()
    {
        renderSystem = new RenderSystem();
        assetManager.load("eti.png", Texture.class);
        assetManager.finishLoading();

        scene = new Scene();

        float cameraSize = 40;
        OrthographicCamera camera = new OrthographicCamera(cameraSize, (float) Gdx.graphics.getHeight()/Gdx.graphics.getWidth() * cameraSize);
        scene.setCamera(camera);

        //create ETI entity
        eti = scene.createEntity();
        Sprite etiSprite = new Sprite( (Texture)assetManager.get("eti.png") );
        etiSprite.setSize(2,2);
        etiSprite.setOriginCenter();

        scene.addComponent(eti, new SpriteComponent(etiSprite));

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(1,1);

        BodyParams bodyParams = BodyParams.builder()
            .type(BodyDef.BodyType.DynamicBody)
            .fixedRotation(true)
            .linearDamping(10f)
            .angularDamping(10f).build();

        FixtureParams fixtureParams = FixtureParams.builder()
            .shape(shape)
            .restitution(0f)
            .density(1f)
            .friction(0f).build();

        scene.getBodyCreator().setBodyDefParams(bodyParams);
        scene.getBodyCreator().setFixtureDefParams(fixtureParams);
        scene.createBodyComponent(eti);
        scene.createFixture(eti);

        //create walls
        int wall = scene.createEntity();

        scene.getBodyCreator().setBodyDefParams(BodyParams.builder().type(BodyDef.BodyType.StaticBody).build());

        scene.createBodyComponent(wall);

        Vector2[] wallPositions =
        {
            new Vector2(0,-scene.getCamera().viewportHeight/2),
            new Vector2(-scene.getCamera().viewportWidth/2, 0),
            new Vector2(0, scene.getCamera().viewportHeight/2),
            new Vector2(scene.getCamera().viewportWidth/2, 0)
        };

        for(int i = 0; i < 4; i++)
        {
            shape.setAsBox(i%2 == 1 ? 0 : scene.getCamera().viewportWidth, i%2 == 0 ? 0 : scene.getCamera().viewportHeight, wallPositions[i], 0);
            scene.getBodyCreator().setFixtureDefParams(FixtureParams.builder().shape(shape).build());
            scene.createFixture(wall);
        }

        shape.dispose();
    }

    @Override
    public void render()
    {
        float speed = 3f;
        Vector2 direction = new Vector2(0,0);
        if(Gdx.input.isKeyPressed(Input.Keys.A))
        {
            direction.x -= 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D))
        {
            direction.x += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W))
        {
            direction.y += 1;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S))
        {
            direction.y -= 1;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
            Gdx.app.exit();
        }

        direction.setLength(1);

        Body etiBody = scene.getComponentStorage(PhysicalBodyComponent.class).getByIndex(eti).body;
        etiBody.applyLinearImpulse(direction.scl(speed), etiBody.getPosition(), true);

        scene.update(Gdx.graphics.getDeltaTime());

        renderSystem.render(scene);

        if(debugMode)
            scene.debugRender();
    }

    @Override
    public void dispose()
    {
        scene.dispose();
        assetManager.dispose();
        renderSystem.dispose();
    }

    private boolean debugMode = false;
    private int eti;
    private RenderSystem renderSystem;
    private final AssetManager assetManager = new AssetManager();
    private Scene scene;
}
