package net.dp.rpg.game.level;

import box2dLight.Light;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.g2d.Animation;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.utils.Array;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.animation.AnimationSet;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.engine.systems.TileConverter;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.animations.PlayerAnimationId;
import net.dp.rpg.game.events.EnterRoomEvent;
import net.dp.rpg.game.level.gui.LevelGui;
import net.dp.rpg.game.scripts.CameraScript;
import net.dp.rpg.game.scripts.DoorScript;
import net.dp.rpg.game.scripts.EnemyScript;
import net.dp.rpg.game.scripts.PlayerScript;

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
        float playerSize = 0.4f;
        int player = scene.createEntity();
        Sprite playerSprite = atlas.createSprite("player");
        Sprite attackSprite = atlas.createSprite("attack");
        playerSprite.setSize(1f,1f);
        playerSprite.setOriginCenter();
        attackSprite.setSize(1.4f, 0.7f);
        attackSprite.setOriginCenter();
        attackSprite.setOrigin(attackSprite.getOriginX(), attackSprite.getOriginY()-0.8f);
        attackSprite.setAlpha(0);

        MultiSpriteComponent playerSpriteComponent = new MultiSpriteComponent();
        playerSpriteComponent.add(playerSprite, false);
        playerSpriteComponent.add(attackSprite, true);
        scene.addComponent(player, playerSpriteComponent);

        //player animation
        Animation<TextureRegion> playerIdleAnimation = new Animation<>(1, atlas.findRegions("player/idle"), Animation.PlayMode.LOOP);
        AnimationSet<PlayerAnimationId> playerAnimationSet = new AnimationSet<>();
        playerAnimationSet.addAnimation(playerIdleAnimation, PlayerAnimationId.IDLE);
        MultiAnimationComponent<PlayerAnimationId> playerAnimations = new MultiAnimationComponent<>();
        scene.addComponent(player, playerAnimations);
        playerAnimations.add(playerAnimationSet);

        CircleShape circle = new CircleShape();
        circle.setRadius(playerSize);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = 10f;
        bodyDef.angularDamping = 10f;
        bodyDef.position.x = (floor.getActiveRoom().origin().x() + 1/2f) * RoomGeometry.CELL_WIDTH;
        bodyDef.position.y = (floor.getActiveRoom().origin().y() + 1/2f) * RoomGeometry.CELL_HEIGHT;

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.restitution = 0f;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f;
        fixtureDef.filter.categoryBits = FixtureCategory.PLAYER;
        scene.createBodyComponent(player, bodyDef);
        Fixture playerFixture = scene.createFixture(player, fixtureDef);

        //player reach sensor (to interact with other objects)
        float playerReach = playerSize * 3f;
        fixtureDef.isSensor = true;
        fixtureDef.density = 0f;
        fixtureDef.filter.categoryBits = FixtureCategory.SENSOR;
        circle.setRadius(playerReach);
        Fixture playerSensorFixture = scene.createFixture(player, fixtureDef);

        scene.addComponent(player, new HealthComponent(playerMaxHealth, playerMaxHealth));

        //player light
        Filter contactFilter = new Filter();
        contactFilter.categoryBits = FixtureCategory.LIGHT;
        contactFilter.maskBits = (short)(FixtureCategory.OBSTACLE | FixtureCategory.WALL);
        Light playerLight = scene.createPointLightComponent(player, 128, new Color(1f, 0.6f, 0.1f, 0.8f), playerSize * 32, false);
        playerLight.setContactFilter(contactFilter);

        //player attack box
        PolygonShape polygon = new PolygonShape();
        polygon.setAsBox(0.4f, 0.2f);
        fixtureDef.density = 0f;
        fixtureDef.isSensor = true;
        fixtureDef.filter.categoryBits = FixtureCategory.PLAYER_ATTACK;
        fixtureDef.filter.maskBits = 0;
        fixtureDef.shape = polygon;
        Fixture playerAttackFixture = scene.createFixture(player, fixtureDef);

        scene.addComponent(player, new ScriptComponent(new PlayerScript(playerFixture, playerAttackFixture, playerSensorFixture)));

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
        bodyDef.position.x = 30;
        bodyDef.position.y = 30;
        bodyDef.fixedRotation = false;
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

        int box2 = scene.cloneEntity(box1);
        scene.getComponentStorage(TransformComponent.class).getByEntity(box2).setPosition(100,80);

        //enemies
        int enemy1 = scene.createEntity();
        bodyDef.position.x = 100;
        bodyDef.position.y = 80;
        bodyDef.fixedRotation = true;
        fixtureDef.friction = 0f;
        fixtureDef.density = 1f;
        circle.setRadius(0.3f);
        fixtureDef.shape = circle;
        fixtureDef.filter.categoryBits = FixtureCategory.ENEMY;

        scene.createBodyComponent(enemy1, bodyDef);
        scene.createFixture(enemy1, fixtureDef);
        Sprite enemySprite = atlas.createSprite("enemy");
        enemySprite.setSize(0.6f,0.6f);
        enemySprite.setOriginCenter();
        scene.addComponent(enemy1, new HealthComponent(50, 50));
        MultiSpriteComponent enemySpriteComponent = new MultiSpriteComponent();
        enemySpriteComponent.sprites.add(enemySprite);
        scene.addComponent(enemy1, enemySpriteComponent);
        scene.addComponent(enemy1, new ScriptComponent(new EnemyScript(player)));
        scene.getComponentStorage(TransformComponent.class).getByEntity(enemy1).setPosition(50,50);

        circle.dispose();
        polygon.dispose();

        //gui
        LevelGui levelGui = new LevelGui(engine, scene, player);

        //inform about first room
        EnterRoomEvent roomEvent = Engine.poolManager.obtain(EnterRoomEvent.class);
        roomEvent.room = floor.getActiveRoom();
        Engine.eventBus.publish(roomEvent);
        Engine.poolManager.free(roomEvent);

        //scene.setAmbientLight(new Color(0.2f,0.2f,0.3f,0.4f)); // ambient light color when no camera light
        scene.setAmbientLight(new Color(0,0,0,0));
    }
}
