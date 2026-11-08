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
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.floor.type.RoomType;
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
    /** System property with a seed to replay, e.g. {@code -Drpg.seed=42}. */
    public static final String SEED_PROPERTY = "rpg.seed";

    private final float playerMaxHealth = 100;
    private final long seed;

    public LevelBuilder(long seed)
    {
        this.seed = seed;
    }

    /** The seed from {@link #SEED_PROPERTY}, or a random one. Logged, so a floor can be replayed. */
    public static LevelBuilder fromSystemProperty()
    {
        String value = System.getProperty(SEED_PROPERTY);
        return new LevelBuilder(value == null || value.isBlank() ? new Random().nextLong() : Long.parseLong(value.trim()));
    }

    public void build(EngineServices engine, Scene scene)
    {
        TextureAtlas atlas = engine.getAssetManager().get("atlases/textures.atlas");

        //tilemap
        Random random = new Random(seed);
        TileConverter tileConverter = new TileConverter();
        FloorArchetype archetype = FloorArchetypes.CATACOMBS;
        TileFloor floor = new TileFloor(archetype, FloorSettings.of(archetype), seed, scene);
        Gdx.app.log("Level", "seed " + seed + " (replay with -D" + SEED_PROPERTY + "=" + seed + ")");
        Engine.eventBus.subscribe(floor, EnterRoomEvent.class);

        DoorManager doorManager = new DoorManager(floor);
        Engine.eventBus.subscribe(doorManager, EnterRoomEvent.class);

        for(TileRoom room : floor.getRooms())
        {
            BodyDef tileBodyDef = new BodyDef();
            tileBodyDef.type = BodyDef.BodyType.StaticBody;
            tileBodyDef.position.x = room.origin().x() * RoomGeometry.CELL_WIDTH;
            // y-up: tile (0, 0) of the room's map is its bottom-left corner, at the origin cell.
            tileBodyDef.position.y = room.origin().y() * RoomGeometry.CELL_HEIGHT;
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
                // Body-local, like the wall chains: the body already sits at the room's origin.
                position.set(x+fieldSize/2,y+fieldSize/2);
                PolygonShape shape = new PolygonShape();
                shape.setAsBox(fieldSize/2, fieldSize/2, position, 0);
                fixtureDef.shape = shape;
                int doorId = scene.createEntity();
                scene.createBodyComponent(doorId, tileBodyDef);
                Fixture doorFixture = scene.createFixture(doorId, fixtureDef);

                DoorScript script = new DoorScript(room, door.direction(), doorFixture, room.doorType(door) == DoorType.LOCKED,
                        new Vector2(worldX(room, x), worldY(room, y)));
                scene.addComponent(doorId, new ScriptComponent(script));
                doorManager.addDoor(room.id(), script);
            }
        }

        //create player entity
        PlayerCreator playerCreator = new PlayerCreator(scene, atlas);
        TileRoom start = floor.getStartRoom();
        int[] spawn = floor.walkableTileNear(start, RoomGeometry.CELL_WIDTH / 2, RoomGeometry.CELL_HEIGHT / 2);
        int player = playerCreator.create(worldX(start, spawn[0]), worldY(start, spawn[1]));

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
        int[] boxTile = floor.walkableTileNear(start, spawn[0] + 3, spawn[1]);
        bodyDef.position.x = worldX(start, boxTile[0]);
        bodyDef.position.y = worldY(start, boxTile[1]);
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
        for(TileRoom room : floor.getRooms())
        {
            int count = enemiesFor(room.node().type(), random);
            for(int i = 0; i < count; i++)
            {
                int[] tile = floor.walkableTileNear(room, 2 + random.nextInt(room.map().width() - 4), 2 + random.nextInt(room.map().height() - 4));
                spiderBuilder.create(worldX(room, tile[0]), worldY(room, tile[1]), floor, room);
            }
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
    }

    /**
     * Placeholder until the content pass: how many spiders a room gets. Will come from the
     * room's content (spawn markers) and the player's profile.
     */
    private static int enemiesFor(RoomType type, Random random)
    {
        return switch(type)
        {
            case NORMAL -> 2 + random.nextInt(3);
            case MINIBOSS -> 6;
            case BOSS -> 10;
            case VAULT, COLLECTIBLE -> random.nextInt(2);
            default -> 0;
        };
    }

    /** World x of the centre of local tile x of the room. */
    private static float worldX(TileRoom room, int tileX)
    {
        return room.origin().x() * RoomGeometry.CELL_WIDTH + tileX + 0.5f;
    }

    private static float worldY(TileRoom room, int tileY)
    {
        return room.origin().y() * RoomGeometry.CELL_HEIGHT + tileY + 0.5f;
    }
}
