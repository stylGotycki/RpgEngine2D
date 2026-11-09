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
import net.dp.rpg.engine.content.ContentSpot;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.dialogue.Dialogue;
import net.dp.rpg.engine.dialogue.DialogueLibrary;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.systems.TileConverter;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.events.EnterRoomEvent;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.dialogue.GameDialogues;
import net.dp.rpg.game.dialogue.NarratorDirector;
import net.dp.rpg.game.enemy.EnemyRegistry;
import net.dp.rpg.game.enemy.GameEnemies;
import net.dp.rpg.game.item.GameItems;
import net.dp.rpg.game.item.ItemDef;
import net.dp.rpg.game.item.LootTable;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.level.entities.DefaultEntityProperties;
import net.dp.rpg.game.level.entities.EnemyEntityCreator;
import net.dp.rpg.game.level.entities.ItemPickupCreator;
import net.dp.rpg.game.level.entities.NpcCreator;
import net.dp.rpg.game.level.entities.player.PlayerCreator;
import net.dp.rpg.game.level.gui.LevelGui;
import net.dp.rpg.game.minimap.MinimapWidget;
import net.dp.rpg.game.scripts.CameraScript;
import net.dp.rpg.game.scripts.DoorScript;
import net.dp.rpg.game.scripts.NpcScript;

import java.util.List;
import java.util.Map;
import java.util.Random;

public class LevelBuilder
{
    /** System property with a seed to replay, e.g. {@code -Drpg.seed=42}. */
    public static final String SEED_PROPERTY = "rpg.seed";

    private final float playerMaxHealth = 100;
    private final long seed;
    private final PlayerStats stats;

    public LevelBuilder(long seed, PlayerStats stats)
    {
        this.seed = seed;
        this.stats = stats;
    }

    public LevelBuilder(long seed)
    {
        this(seed, PlayerStats.rolled(RandomSource.derive(seed, "player-start")));
    }

    /** The seed from {@link #SEED_PROPERTY}, or a random one. Logged, so a floor can be replayed. */
    public static LevelBuilder fromSystemProperty()
    {
        String value = System.getProperty(SEED_PROPERTY);
        long seed = value == null || value.isBlank() ? new Random().nextLong() : Long.parseLong(value.trim());
        return new LevelBuilder(seed, PlayerStats.rolled(RandomSource.derive(seed, "player-start")));
    }

    public void build(EngineServices engine, Scene scene)
    {
        TextureAtlas atlas = engine.getAssetManager().get("atlases/textures.atlas");

        //tilemap
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
        DialogueController dialogues = new DialogueController();
        DialogueLibrary dialogueLibrary = GameDialogues.library();
        PlayerCreator playerCreator = new PlayerCreator(scene, atlas, stats, dialogues);
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
        EnemyEntityCreator enemyCreator = new EnemyEntityCreator(scene, atlas, player, dialogues);
        NpcCreator npcCreator = new NpcCreator(scene, atlas);
        EnemyRegistry enemies = GameEnemies.registry();
        // Content comes from the generator (E1): spots on floor tiles, enemies away from doors.
        ItemPickupCreator itemCreator = new ItemPickupCreator(scene, atlas);
        LootTable loot = GameItems.loot();
        int maxDepth = floor.getRooms().stream().mapToInt(room -> room.node().depth()).max().orElse(1);
        for(TileRoom room : floor.getRooms())
        {
            double depth = maxDepth == 0 ? 0 : (double) room.node().depth() / maxDepth;
            Random itemRandom = RandomSource.derive(seed, "items:" + room.id());
            List<ContentSpot> offers = room.content().spots().stream()
                    .filter(spot -> spot.tag().equals(ContentTags.SHOP_OFFER)).toList();
            List<ItemDef> offerItems = loot.shopOffers(offers.size(), itemRandom, depth);
            for(int i = 0; i < offers.size(); i++)
                itemCreator.create(offerItems.get(i), worldX(room, offers.get(i).x()), worldY(room, offers.get(i).y()), "shop-" + room.id());

            for(ContentSpot spot : room.content().spots())
            {
                float x = worldX(room, spot.x());
                float y = worldY(room, spot.y());
                switch(spot.kind())
                {
                    case ENEMY -> enemyCreator.create(enemies.get(spot.tag()), x, y, floor, room);
                    case ITEM ->
                    {
                        if(!spot.tag().equals(ContentTags.SHOP_OFFER))
                            itemCreator.create(loot.resolve(spot.tag(), itemRandom, depth), x, y, null);
                    }
                    // NPCs (E4) are coloured markers until they exist.
                    case NPC -> npcCreator.create("npc/giver", x, y, questGiver(spot, dialogueLibrary));
                }
            }
        }
        itemCreator.dispose();

        enemyCreator.dispose();
        npcCreator.dispose();
        playerCreator.dispose();

        polygon.dispose();

        //gui
        // Subscribed before the first EnterRoomEvent below, like the rest of the room listeners.
        MinimapWidget minimap = new MinimapWidget(floor.getLayout(), atlas, 260, 180);
        Engine.eventBus.subscribe(minimap, EnterRoomEvent.class);
        LevelGui levelGui = new LevelGui(engine, scene, player, minimap, stats, atlas, dialogues);

        NarratorDirector narrator = new NarratorDirector(floor, dialogues, dialogueLibrary,
                Map.of("boss", GameEnemies.registry().get(ContentTags.BOSS).name()));
        Engine.eventBus.subscribe(narrator, EnterRoomEvent.class);
        narrator.floorStart();

        //inform about first room
        Engine.eventBus.publish(EnterRoomEvent.class, e -> e.room = floor.getActiveRoom());

        //scene.setAmbientLight(new Color(0.2f,0.2f,0.3f,0.4f)); // ambient light color when no camera light
        scene.setAmbientLight(new Color(0,0,0,0));
    }

    // Until quests are generated (E5) the offer is a placeholder; the story is the giver's own.
    private NpcScript questGiver(ContentSpot spot, DialogueLibrary library)
    {
        List<Dialogue> stories = library.withPrefix("giver.story.");
        Dialogue story = stories.get(RandomSource.derive(seed, "giver:" + spot.questId()).nextInt(stories.size()));
        String name = story.node(story.start()).speaker();
        NpcScript npc = new NpcScript(NpcScript.Role.QUEST_GIVER, name, story.id(), spot.questId());
        npc.setVariable("quest", "Pomożesz mi? Treść zadania pojawi się, gdy powstaną questy (E5).");
        npc.setVariable("progress", "");
        return npc;
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
