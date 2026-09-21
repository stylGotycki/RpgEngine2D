package net.dp.rpg.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.bodyCreator.BodyParams;
import net.dp.rpg.engine.bodyCreator.FixtureParams;
import net.dp.rpg.engine.components.CameraComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.scripts.EtiScript;

public class LevelBuilder
{
    public void build(EngineServices engine, Scene scene)
    {
        //create ETI entity
        int eti = scene.createEntity();
        Sprite etiSprite = new Sprite( (Texture) engine.getAssetManager().get("eti.png") );
        etiSprite.setSize(0.8f,0.8f);
        etiSprite.setOriginCenter();

        scene.addComponent(eti, new SpriteComponent(etiSprite));

        PolygonShape shape = new PolygonShape();
        shape.setAsBox(0.4f,0.4f);

        BodyParams bodyParams = BodyParams.builder()
            .type(BodyDef.BodyType.DynamicBody)
            .fixedRotation(true)
            .linearDamping(10f)
            .angularDamping(10f).build();

        FixtureParams fixtureParams = FixtureParams.builder()
            .shape(shape)
            .restitution(0f)
            .density(0f)
            .friction(0f).build();

        bodyParams.position = new Vector2(10,10);
        scene.getBodyCreator().setBodyDefParams(bodyParams);
        scene.getBodyCreator().setFixtureDefParams(fixtureParams);
        scene.createBodyComponent(eti);
        scene.createFixture(eti);

        float cameraSize = 40;
        OrthographicCamera camera = new OrthographicCamera(cameraSize, (float) Gdx.graphics.getHeight()/Gdx.graphics.getWidth() * cameraSize);
        scene.addComponent(eti, new CameraComponent(camera));
        scene.setActiveCamera(eti);

        //create walls
//        int wall = scene.createEntity();
//
//        scene.getBodyCreator().setBodyDefParams(BodyParams.builder().type(BodyDef.BodyType.StaticBody).build());
//
//        scene.createBodyComponent(wall);
//
//        Vector2[] wallPositions =
//            {
//                new Vector2(0,-scene.getActiveCamera().viewportHeight/2),
//                new Vector2(-scene.getActiveCamera().viewportWidth/2, 0),
//                new Vector2(0, scene.getActiveCamera().viewportHeight/2),
//                new Vector2(scene.getActiveCamera().viewportWidth/2, 0)
//            };
//
//        for(int i = 0; i < 4; i++)
//        {
//            shape.setAsBox(i%2 == 1 ? 0 : scene.getActiveCamera().viewportWidth, i%2 == 0 ? 0 : scene.getActiveCamera().viewportHeight, wallPositions[i], 0);
//            scene.getBodyCreator().setFixtureDefParams(FixtureParams.builder().shape(shape).build());
//            scene.createFixture(wall);
//        }

        scene.addComponent(eti, new ScriptComponent(new EtiScript()));

        TileFloor floor = new TileFloor(20260918L, 12);
        floor.loadRoomFromFile();

        scene.setTileMap(floor.getActiveRoom().map(), floor.getTileSystem());

        shape.dispose();
    }
}
