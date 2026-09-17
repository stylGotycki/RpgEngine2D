package net.dp.rpg.game;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.PolygonShape;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.dp.rpg.engine.AbstractGame;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.bodyCreator.BodyParams;
import net.dp.rpg.engine.bodyCreator.FixtureParams;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.game.scripts.EtiScript;
import net.dp.rpg.game.scripts.ToggleFullScreenScript;

public class Game extends AbstractGame
{
    @Override
    public void create()
    {
        getEngine().getAssetManager().load("eti.png", Texture.class);
        getEngine().getAssetManager().load("skins/default/default.json", Skin.class);

        getEngine().getAssetManager().finishLoading();

        Scene scene = getEngine().createScene();

        MenuBuilder menuBuilder = new MenuBuilder();
        menuBuilder.build(getEngine(), scene);

        getEngine().switchScene(scene);

        getEngine().setGlobalScript(new ToggleFullScreenScript());
    }
}
