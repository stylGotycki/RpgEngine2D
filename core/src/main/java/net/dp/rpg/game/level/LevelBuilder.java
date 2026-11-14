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
import net.dp.rpg.engine.Script;
import net.dp.rpg.engine.adaptation.Adaptation;
import net.dp.rpg.engine.content.ContentKind;
import net.dp.rpg.engine.content.ContentSpot;
import net.dp.rpg.engine.puzzle.PuzzleSpec;
import net.dp.rpg.engine.quest.Quest;
import net.dp.rpg.engine.quest.QuestGoal;
import net.dp.rpg.engine.quest.QuestPlanner;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.content.ContentTags;
import net.dp.rpg.engine.dialogue.Dialogue;
import net.dp.rpg.engine.dialogue.DialogueLibrary;
import net.dp.rpg.engine.floor.RandomSource;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.graph.DoorType;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.dp.rpg.engine.systems.TileConverter;
import net.dp.rpg.engine.tile.room.RoomEdge;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.adaptation.AdaptationOverlay;
import net.dp.rpg.game.adaptation.AdaptationScript;
import net.dp.rpg.game.adaptation.GameSession;
import net.dp.rpg.game.adaptation.MetricsCollector;
import net.dp.rpg.game.level.events.EnterRoomEvent;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.dialogue.GameDialogues;
import net.dp.rpg.game.dialogue.NarratorDirector;
import net.dp.rpg.game.enemy.EnemyRegistry;
import net.dp.rpg.game.enemy.GameEnemies;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.level.events.InteractEvent;
import net.dp.rpg.game.level.events.HealthChangeEvent;
import net.dp.rpg.game.item.GameItems;
import net.dp.rpg.game.quest.QuestRewards;
import net.dp.rpg.game.quest.QuestTexts;
import net.dp.rpg.game.quest.QuestTracker;
import net.dp.rpg.game.scripts.ItemPickupScript;
import net.dp.rpg.game.item.ItemDef;
import net.dp.rpg.game.item.LootTable;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.level.entities.DefaultEntityProperties;
import net.dp.rpg.game.level.entities.EnemyEntityCreator;
import net.dp.rpg.game.level.entities.ItemPickupCreator;
import net.dp.rpg.game.level.entities.NpcCreator;
import net.dp.rpg.game.level.entities.player.PlayerCreator;
import net.dp.rpg.game.level.events.PlayerDeathEvent;
import net.dp.rpg.game.level.gui.LevelGui;
import net.dp.rpg.game.minimap.MinimapWidget;
import net.dp.rpg.game.scripts.CameraScript;
import net.dp.rpg.game.scripts.DoorScript;
import net.dp.rpg.game.scripts.NpcScript;
import net.dp.rpg.game.scripts.PuzzleScript;
import net.dp.rpg.game.scripts.PlayerScript;

import java.util.ArrayList;
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
    private final GameSession session;

    public LevelBuilder(long seed, PlayerStats stats, GameSession session)
    {
        this.seed = seed;
        this.stats = stats;
        this.session = session;
    }

    // A new game: rolled stats and a fresh session, so the first floor is the neutral one.
    public LevelBuilder(long seed)
    {
        this(seed, PlayerStats.rolled(RandomSource.derive(seed, "player-start")), GameSession.start());
    }

    /** The seed from {@link #SEED_PROPERTY}, or a random one. Logged, so a floor can be replayed. */
    public static LevelBuilder fromSystemProperty()
    {
        String value = System.getProperty(SEED_PROPERTY);
        long seed = value == null || value.isBlank() ? new Random().nextLong() : Long.parseLong(value.trim());
        return new LevelBuilder(seed);
    }

    public void build(EngineServices engine, Scene scene)
    {
        TextureAtlas atlas = engine.getAssetManager().get("atlases/textures.atlas");

        //tilemap
        TileConverter tileConverter = new TileConverter();
        // E6: the floor is generated from the session's profile; the first one from the neutral one.
        FloorArchetype archetype = session.adaptive().archetype();
        Adaptation adaptation = session.adaptive().current();
        TileFloor floor = new TileFloor(archetype, adaptation.floor(), adaptation.content(), adaptation.quests(), seed, scene);
        Gdx.app.log("Level", "floor " + session.adaptive().floor() + ", seed " + seed + " (replay with -D" + SEED_PROPERTY + "=" + seed + ")");
        adaptation.reasons().forEach(reason -> Gdx.app.log("Adaptation", reason));
        // Before the first EnterRoomEvent, so the start room counts as visited.
        session.beginFloor(seed, new MetricsCollector(floor));
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

        // Puzzles: floor sprites (underlay), drawn under the player and everything else.
        List<PuzzleScript> puzzles = new ArrayList<>();
        BodyDef puzzleBody = new BodyDef();
        puzzleBody.type = BodyDef.BodyType.StaticBody;
        for(TileRoom room : floor.getRooms())
        {
            for(PuzzleSpec spec : room.content().puzzles())
            {
                PuzzleScript puzzle = new PuzzleScript(spec, room, floor, atlas, solved -> puzzleReward(scene, atlas, solved));
                // One entity per tile, built like an item pickup (the path every visible object
                // takes): a static body at the tile's centre gives the transform the sprite
                // follows. No fixture, so nothing collides with it. Rotation stays the script's.
                for(Sprite sprite : puzzle.sprites())
                {
                    int tile = scene.createEntity();
                    puzzleBody.position.set(sprite.getX() + sprite.getOriginX(), sprite.getY() + sprite.getOriginY());
                    scene.createBodyComponent(tile, puzzleBody);
                    scene.addComponent(tile, new SpriteComponent(sprite, true, true));
                }
                int entity = scene.createEntity();
                scene.addComponent(entity, new ScriptComponent(puzzle));
                Engine.eventBus.subscribe(puzzle, InteractEvent.class);
                puzzles.add(puzzle);
            }
        }

        //create player entity
        DialogueController dialogues = new DialogueController();
        DialogueLibrary dialogueLibrary = GameDialogues.library();
        PlayerCreator playerCreator = new PlayerCreator(scene, atlas, stats, dialogues);
        TileRoom start = floor.getStartRoom();
        int[] spawn = floor.walkableTileNear(start, RoomGeometry.CELL_WIDTH / 2, RoomGeometry.CELL_HEIGHT / 2);
        int player = playerCreator.create(worldX(start, spawn[0]), worldY(start, spawn[1]));
        puzzles.forEach(puzzle -> puzzle.setPlayer(player));

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
        QuestTracker quests = new QuestTracker(floor.getQuests(), floor.getLayout(),
                new QuestTexts(enemies, GameItems.registry()), new QuestTracker.Hooks()
        {
            @Override
            public void reward(Quest quest)
            {
                Random random = RandomSource.derive(seed, "reward:" + quest.id());
                String text = switch(QuestRewards.roll(random))
                {
                    case POTION -> drop(loot.resolve(ContentTags.POTION, random, quest.depth()));
                    case EQUIPMENT -> drop(questReward(quest, loot, random));
                    case SKILL_POINT ->
                    {
                        if(scriptOf(scene, player) instanceof PlayerScript playerScript)
                            playerScript.addSkillPoint();
                        yield "darmowy punkt umiejętności (X)";
                    }
                    case HEAL ->
                    {
                        HealthComponent health = scene.getComponentStorage(HealthComponent.class).getByEntity(player);
                        health.addHealth(health.maxHealth);
                        Engine.eventBus.publish(HealthChangeEvent.class, e -> e.currentHealth = health.health);
                        yield "pełne leczenie";
                    }
                };
                Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = "Nagroda: " + text);
            }

            private String drop(ItemDef item)
            {
                ItemPickupCreator creator = new ItemPickupCreator(scene, atlas);
                Vector2 at = scene.getComponentStorage(TransformComponent.class).getByEntity(player).position;
                creator.create(item, at.x, at.y, null);
                creator.dispose();
                return item.name();
            }

            @Override
            public void respawnChallenge(Quest quest, QuestGoal goal)
            {
                TileRoom room = floor.getRooms().get(goal.rooms().get(0));
                EnemyEntityCreator creator = new EnemyEntityCreator(scene, atlas, player, dialogues);
                for(ContentSpot spot : room.content().ofKind(ContentKind.ENEMY))
                    creator.create(enemies.get(spot.tag()), worldX(room, spot.x()), worldY(room, spot.y()), floor, room);
                creator.dispose();
                Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = "Przeciwnicy wrócili. Spróbuj jeszcze raz!");
            }
        });
        for(Class<? extends GameEvent> event : QuestTracker.EVENTS)
            Engine.eventBus.subscribe(quests, event);

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
                        if(spot.tag().startsWith(ContentTags.QUEST_ITEM_PREFIX))
                        {
                            ItemDef item = GameItems.registry().get(spot.tag().substring(ContentTags.QUEST_ITEM_PREFIX.length()));
                            int entity = itemCreator.create(item, x, y, null, spot.questId());
                            if(scriptOf(scene, entity) instanceof ItemPickupScript pickup)
                                quests.registerPickup(pickup);
                        }
                        else if(!spot.tag().equals(ContentTags.SHOP_OFFER))
                            itemCreator.create(loot.resolve(spot.tag(), itemRandom, depth), x, y, null);
                    }
                    case NPC ->
                    {
                        if(spot.tag().startsWith(ContentTags.TALKER_PREFIX))
                        {
                            String dialogueId = spot.tag().substring(ContentTags.TALKER_PREFIX.length());
                            NpcScript talker = new NpcScript(NpcScript.Role.TALK, TALKERS.getOrDefault(dialogueId, "Nieznajomy"), dialogueId, spot.questId());
                            npcCreator.create(dialogueId.equals("talk.q.sentinel") ? "npc/villager" : "npc/sage", x, y, talker);
                            quests.registerTalker(spot.questId(), talker);
                        }
                        else
                        {
                            NpcScript giver = questGiver(spot, dialogueLibrary);
                            npcCreator.create("npc/giver", x, y, giver);
                            quests.registerGiver(spot.questId(), giver);
                        }
                    }
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
        minimap.setQuestRooms(quests::markedRooms);
        LevelGui levelGui = new LevelGui(engine, scene, player, minimap, stats, atlas, dialogues, quests);

        // F3 overlay over the rest of the GUI, left middle; F5 builds the next floor from it.
        AdaptationOverlay overlay = new AdaptationOverlay(session, atlas, engine.getAssetManager().get("skins/default/default.json", Skin.class));
        Table overlayTable = new Table();
        overlayTable.setFillParent(true);
        overlayTable.left().padLeft(16);
        overlayTable.add(overlay);
        scene.getGui().getStage().addActor(overlayTable);
        // One scene script per scene: AdaptationScript extends LevelScript, so the player's
        // death (dev) and F3/F5 both work.
        AdaptationScript levelScript = new AdaptationScript(session, stats, overlay);
        Engine.eventBus.subscribe(levelScript, PlayerDeathEvent.class);
        scene.setSceneScript(levelScript);

        NarratorDirector narrator = new NarratorDirector(floor, dialogues, dialogueLibrary,
                Map.of("boss", GameEnemies.registry().get(ContentTags.BOSS).name()));
        Engine.eventBus.subscribe(narrator, EnterRoomEvent.class);
        narrator.floorStart();

        //inform about first room
        Engine.eventBus.publish(EnterRoomEvent.class, e -> e.room = floor.getActiveRoom());

        //scene.setAmbientLight(new Color(0.2f,0.2f,0.3f,0.4f)); // ambient light color when no camera light
        scene.setAmbientLight(new Color(0,0,0,0));

        scene.setAttackCategories(FixtureCategory.ENEMY_ATTACK, FixtureCategory.PLAYER_ATTACK);
        scene.setBodyCategories(FixtureCategory.PLAYER, FixtureCategory.ENEMY);

    }

    // Until quests are generated (E5) the offer is a placeholder; the story is the giver's own.
    private NpcScript questGiver(ContentSpot spot, DialogueLibrary library)
    {
        List<Dialogue> stories = library.withPrefix("giver.story.");
        Dialogue story = stories.get(RandomSource.derive(seed, "giver:" + spot.questId()).nextInt(stories.size()));
        String name = story.node(story.start()).speaker();
        return new NpcScript(NpcScript.Role.QUEST_GIVER, name, story.id(), spot.questId());
    }

    private static final Map<String, String> TALKERS = Map.of(
            "talk.q.sentinel", "Strażnik",
            "talk.q.riddle", "Zagadkowy mędrzec");

    // A reward the player can use now: an item whose requirements they meet.
    private ItemDef questReward(Quest quest, LootTable loot, Random random)
    {
        for(int attempt = 0; attempt < 12; attempt++)
        {
            ItemDef item = loot.resolve(quest.rewardTag(), random, quest.depth());
            if(stats.missingRequirement(item).isEmpty())
                return item;
        }
        return loot.resolve(ContentTags.POTION, random, quest.depth());
    }

    // A puzzle room's own puzzle pays out on the spot; a quest's puzzle pays through the quest.
    private void puzzleReward(Scene scene, TextureAtlas atlas, PuzzleScript puzzle)
    {
        if(puzzle.spec().questId() != null)
            return;
        PuzzleSpec spec = puzzle.spec();
        Random random = RandomSource.derive(seed, "puzzle-reward:" + puzzle.room().id());
        ItemDef item = GameItems.loot().resolve(ContentTags.LOOT, random, 0.5);
        ItemPickupCreator creator = new ItemPickupCreator(scene, atlas);
        creator.create(item, worldX(puzzle.room(), spec.x() + spec.width() / 2), worldY(puzzle.room(), spec.y() + spec.height() / 2), null);
        creator.dispose();
    }

    private static Script scriptOf(Scene scene, int entity)
    {
        ScriptComponent component = scene.getComponentStorage(ScriptComponent.class).getByEntity(entity);
        return component == null ? null : component.script;
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
