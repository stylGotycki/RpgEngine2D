package net.dp.rpg.game.level;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.physics.box2d.*;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.*;
import net.dp.rpg.game.FixtureCategory;
import net.dp.rpg.game.TileFloor;
import net.dp.rpg.game.scripts.EnemyScript;
import net.dp.rpg.game.scripts.PlayerScript;

public class LevelBuilder
{
    private final float playerMaxHealth = 100;

    public void build(EngineServices engine, Scene scene)
    {
        //create player entity
        int player = scene.createEntity();
        Sprite playerSprite = new Sprite( (Texture) engine.getAssetManager().get("player.png") );
        Sprite attackSprite = new Sprite( (Texture) engine.getAssetManager().get("attack.png") );
        playerSprite.setSize(0.8f,0.8f);
        playerSprite.setOriginCenter();
        attackSprite.setSize(1.4f, 0.7f);
        attackSprite.setOriginCenter();
        attackSprite.setOrigin(attackSprite.getOriginX(), attackSprite.getOriginY()-0.8f);
        attackSprite.setAlpha(0);

        SpriteComponent playerSpriteComponent = new SpriteComponent();
        playerSpriteComponent.add(playerSprite, false);
        playerSpriteComponent.add(attackSprite, true);
        scene.addComponent(player, playerSpriteComponent);

        CircleShape circle = new CircleShape();
        circle.setRadius(0.4f);

        BodyDef bodyDef = new BodyDef();
        bodyDef.type = BodyDef.BodyType.DynamicBody;
        bodyDef.fixedRotation = true;
        bodyDef.linearDamping = 10f;
        bodyDef.angularDamping = 10f;
        bodyDef.position.x = 10;
        bodyDef.position.y = 10;

        FixtureDef fixtureDef = new FixtureDef();
        fixtureDef.shape = circle;
        fixtureDef.restitution = 0f;
        fixtureDef.density = 1f;
        fixtureDef.friction = 0f;
        fixtureDef.filter.categoryBits = FixtureCategory.PLAYER;
        scene.createBodyComponent(player, bodyDef);
        scene.createFixture(player, fixtureDef);
        scene.addComponent(player, new HealthComponent(playerMaxHealth, playerMaxHealth));

        PolygonShape polygon = new PolygonShape();
        polygon.setAsBox(0.4f, 0.2f);
        fixtureDef.density = 0f;
        fixtureDef.isSensor = true;
        fixtureDef.filter.categoryBits = FixtureCategory.PLAYER_ATTACK;
        fixtureDef.filter.maskBits = 0;
        fixtureDef.shape = polygon;
        scene.createFixture(player, fixtureDef);

        //tilemap
        TileFloor floor = new TileFloor(20260918L, 12);
        floor.loadRoomFromFile();

        scene.setTileMap(floor.getActiveRoom().map(), floor.getTileSystem());

        //camera
        float cameraHeight = 13;
        int cameraEntity = scene.createEntity();
        OrthographicCamera camera = new OrthographicCamera((float) Gdx.graphics.getWidth()/Gdx.graphics.getHeight() * cameraHeight, cameraHeight);
        scene.addComponent(cameraEntity, new CameraComponent(camera));
        scene.addComponent(cameraEntity, new BoundingComponent(0,0,scene.getTileMap().width(), scene.getTileMap().height(), camera.viewportWidth, cameraHeight));
        scene.setActiveCamera(cameraEntity);

        scene.addComponent(player, new ScriptComponent(new PlayerScript()));
        scene.addComponent(cameraEntity, new FollowComponent(player, 8));

        //boxes
        bodyDef.position.x = 12;
        bodyDef.position.y = 10;
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
        Sprite boxSprite = new Sprite( (Texture) engine.getAssetManager().get("box.png"));
        boxSprite.setSize(1,1);
        boxSprite.setOriginCenter();
        SpriteComponent boxSpriteComponent = new SpriteComponent();
        boxSpriteComponent.add(boxSprite, false);
        scene.addComponent(box1, boxSpriteComponent);

        int box2 = scene.cloneEntity(box1);
        scene.getComponentStorage(TransformComponent.class).getByEntity(box2).setPosition(14,10);

        //enemies
        int enemy1 = scene.createEntity();
        bodyDef.position.x = 20;
        bodyDef.position.y = 20;
        bodyDef.fixedRotation = true;
        fixtureDef.friction = 0f;
        fixtureDef.density = 1f;
        circle.setRadius(0.3f);
        fixtureDef.shape = circle;
        fixtureDef.filter.categoryBits = FixtureCategory.ENEMY;

        scene.createBodyComponent(enemy1, bodyDef);
        scene.createFixture(enemy1, fixtureDef);
        Sprite enemySprite = new Sprite( (Texture) engine.getAssetManager().get("enemy.png"));
        enemySprite.setSize(0.6f,0.6f);
        enemySprite.setOriginCenter();
        scene.addComponent(enemy1, new HealthComponent(50, 50));
        SpriteComponent enemySpriteComponent = new SpriteComponent();
        enemySpriteComponent.sprites.add(enemySprite);
        scene.addComponent(enemy1, enemySpriteComponent);
        scene.addComponent(enemy1, new ScriptComponent(new EnemyScript(player)));
        scene.getComponentStorage(TransformComponent.class).getByEntity(enemy1).setPosition(20,20);

        circle.dispose();
        polygon.dispose();

        //gui
        Gui gui = scene.getGui();
        Table mainTable = gui.getMainTable();
        mainTable.top().left();
        HealthBar healthBar = new HealthBar(playerMaxHealth, engine.getAssetManager().get("skins/default/default.json"));
        engine.getEventBus().subscribe(healthBar, HealthChangeEvent.class);
        mainTable.add(healthBar).pad(16).width(playerMaxHealth*4);
    }
}
