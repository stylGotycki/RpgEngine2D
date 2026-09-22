package net.dp.rpg.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Rectangle;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.CircleShape;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.bodyCreator.BodyParams;
import net.dp.rpg.engine.bodyCreator.FixtureParams;
import net.dp.rpg.engine.components.CameraComponent;
import net.dp.rpg.engine.components.FollowComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.scripts.CameraScript;
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

        TileFloor floor = new TileFloor(20260918L, 12);
        floor.loadRoomFromFile();

        scene.setTileMap(floor.getActiveRoom().map(), floor.getTileSystem());

        Rectangle camBounds = new Rectangle(0,0,scene.getTileMap().width(), scene.getTileMap().height());
        float cameraHeight = 13;
        int cameraEntity = scene.createEntity();
        OrthographicCamera camera = new OrthographicCamera((float) Gdx.graphics.getWidth()/Gdx.graphics.getHeight() * cameraHeight, cameraHeight);
        scene.addComponent(cameraEntity, new CameraComponent(camera, camBounds));
        scene.setActiveCamera(cameraEntity);

        scene.addComponent(eti, new ScriptComponent(new EtiScript()));
        scene.addComponent(cameraEntity, new FollowComponent(eti, 8));

        shape.dispose();
    }
}
