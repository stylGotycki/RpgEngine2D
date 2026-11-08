package net.dp.rpg.game.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.systems.TileConverter;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.events.EnterRoomEvent;
import net.dp.rpg.game.level.entities.DefaultEntityProperties;
import net.dp.rpg.game.level.entities.SpiderEntityCreator;
import net.dp.rpg.game.level.entities.player.PlayerCreator;
import net.dp.rpg.game.level.gui.LevelGui;
import net.dp.rpg.game.scripts.CameraScript;
import net.dp.rpg.game.scripts.DoorScript;

import java.util.Random;

public class LevelBuilder
{
    private final float playerMaxHealth = 100;

    public void build(EngineServices engine, Scene scene)
    {
        TextureAtlas atlas = engine.getAssetManager().get("atlases/textures.atlas");

        //tilemap
        Random random = new Random();
        TileConverter tileConverter = new TileConverter();
        TileFloor floor = new TileFloor(3, 12, scene);
        //floor.loadRoomFromFile();
        Engine.eventBus.subscribe(floor, EnterRoomEvent.class);

        DoorManager doorManager = new DoorManager();
        Engine.eventBus.subscribe(doorManager, EnterRoomEvent.class);

        for(TileRoom room : floor.getRooms())
        {
            BodyDef tileBodyDef = new BodyDef();
            tileBodyDef.type = BodyDef.BodyType.StaticBody;
            tileBodyDef.position.x = room.origin().x() * RoomGeometry.CELL_WIDTH;
            tileBodyDef.position.y = (floor.getGridHeight() - room.origin().y()) * RoomGeometry.CELL_HEIGHT - room.shape().tileHeight();
            Body tileMapBody = scene.getPhysicalWorld().createBody(tileBodyDef);

            Array<FixtureDef> fixtureDefs = tileConverter.creatFixtureDefsOnNotWalkable(room.map(), floor.getTileSystem());

            if (!fixtureDefs.isEmpty())
            {
                for (FixtureDef tileFixtureDef : fixtureDefs)
                {
                    tileMapBody.createFixture(tileFixtureDef);
                    tileFixtureDef.shape.dispose();
                }
            }

            float fieldSize = 1f;
            Vector2 position = new Vector2();
            for(RoomEdge door : room.doors())
            {
                int x = door.doorTileX();
                int y = door.doorTileY();
                FixtureDef fixtureDef = new FixtureDef();
                fixtureDef.filter.categoryBits = FixtureCategory.OBSTACLE;
                position.set(x+fieldSize/2,y+fieldSize/2);
                TileConverter.convertToWorldCoords(position, fieldSize, room.map().height());
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(fieldSize/2, fieldSize/2, position, 0);
                fixtureDef.shape = shape;
                int doorId = scene.createEntity();
                scene.createBodyComponent(doorId, tileBodyDef);
                Fixture doorFixture = scene.createFixture(doorId, fixtureDef);

                DoorScript script = new DoorScript(room, door.direction(), doorFixture);
                scene.addComponent(doorId, new ScriptComponent(script));
                doorManager.addDoor(room.id(), script);
            }
        }

        //create player entity
        PlayerCreator playerCreator = new PlayerCreator(scene, atlas);
        float playerXPos = (floor.getActiveRoom().origin().x() + 1/2f) * RoomGeometry.CELL_WIDTH;
        float playerYPos = (floor.getActiveRoom().origin().y() + 1/2f) * RoomGeometry.CELL_HEIGHT;
        int player = playerCreator.create(playerXPos, playerYPos);

        //camera
        float cameraHeight = 13;
        int cameraEntity = scene.createEntity();
        OrthographicCamera camera = new OrthographicCamera((float) Gdx.graphics.getWidth()/Gdx.graphics.getHeight() * cameraHeight, cameraHeight);
        scene.addComponent(cameraEntity, new CameraComponent(camera));
        BoundingComponent bounds = new BoundingComponent(0,0,scene.getTileMap().width(), scene.getTileMap().height(), camera.viewportWidth, cameraHeight);
        bounds.lerp = 12f;
        bounds.insideTolerance = 1f;
        bounds.useLerp = false;
        bounds.speed = 16;
        scene.addComponent(cameraEntity, bounds);
        scene.setActiveCamera(cameraEntity);
        scene.addComponent(cameraEntity, new FollowComponent(player, 8f));
        CameraScript cameraScript = new CameraScript(player);
        scene.addComponent(cameraEntity, new ScriptComponent(cameraScript));
        Engine.eventBus.subscribe(cameraScript, EnterRoomEvent.class);
        scene.createPointLightComponent(cameraEntity, 64, new Color(0.3f,0.3f,0.6f,1f), camera.viewportWidth, true);

        //boxes
        BodyDef bodyDef = new BodyDef();
        FixtureDef fixtureDef = new FixtureDef();
        PolygonShape polygon = new PolygonShape();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.position.x = 30;
        bodyDef.position.y = 30;
        bodyDef.fixedRotation = false;
        bodyDef.linearDamping = DefaultEntityProperties.LINEAR_DAMPING;
        bodyDef.angularDamping = DefaultEntityProperties.ANGULAR_DAMPING;
        fixtureDef.restitution = 0f;
        fixtureDef.friction = 10f;
        fixtureDef.density = 1f;
        fixtureDef.filter.categoryBits = FixtureCategory.OBSTACLE;
        fixtureDef.isSensor = false;
        fixtureDef.shape = polygon;
        fixtureDef.filter.maskBits = (short) 0xFFFF;

        polygon.setAsBox(0.5f, 0.5f);

        int box1 = scene.createEntity();
        scene.createBodyComponent(box1, bodyDef);
        scene.createFixture(box1, fixtureDef);
        Sprite boxSprite = atlas.createSprite("box");
        boxSprite.setSize(1,1);
        boxSprite.setOriginCenter();
        MultiSpriteComponent boxSpriteComponent = new MultiSpriteComponent();
        boxSpriteComponent.add(boxSprite, false);
        scene.addComponent(box1, boxSpriteComponent);

        //enemies
        SpiderEntityCreator spiderBuilder = new SpiderEntityCreator(scene, atlas, player);
        for(int i = 0; i < 100; i++)
        {
            spiderBuilder.create(45+i,60);
        }

        spiderBuilder.dispose();
        playerCreator.dispose();

        polygon.dispose();

        //gui
        LevelGui levelGui = new LevelGui(engine, scene, player);

        //inform about first room
        Engine.eventBus.publish(EnterRoomEvent.class, e -> e.room = floor.getActiveRoom());

        //scene.setAmbientLight(new Color(0.2f,0.2f,0.3f,0.4f)); // ambient light color when no camera light
        scene.setAmbientLight(new Color(0,0,0,0));

        scene.setAttackCategories(FixtureCategory.ENEMY_ATTACK, FixtureCategory.PLAYER_ATTACK);
        scene.setBodyCategories(FixtureCategory.PLAYER, FixtureCategory.ENEMY);
    }
}
