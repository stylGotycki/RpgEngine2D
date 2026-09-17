package net.dp.rpg.game;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.game.scripts.AppCloseScript;

import java.awt.*;

public class MenuBuilder
{
    public void build(EngineServices engine, Scene scene)
    {
        LevelBuilder levelBuilder = new LevelBuilder();

        Gui gui = scene.getGui();
        Table guiTable = gui.getMainTable();

        Skin guiSkin = engine.getAssetManager().get("skins/default/default.json");

//        FreeTypeFontGenerator generator = new FreeTypeFontGenerator(Gdx.files.internal("EspiaHungaro.otf"));
//        FreeTypeFontGenerator.FreeTypeFontParameter parameter = new FreeTypeFontGenerator.FreeTypeFontParameter();
//        parameter.size = 96;
//        BitmapFont font = generator.generateFont(parameter);
//        generator.dispose();
//        guiSkin.add("EspiaHungaro.otf", font, BitmapFont.class);
//
//        guiSkin.get(Label.LabelStyle.class).font = guiSkin.getFont("EspiaHungaro.otf");

        // title
        Label title = new Label("THE DUNGEON", guiSkin);

        // buttons
        TextButton playButton = new TextButton("Play", guiSkin);
        TextButton exitButton = new TextButton("Exit", guiSkin);

        playButton.addListener(new ChangeListener()
        {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                Scene scene = engine.createScene();
                levelBuilder.build(engine, scene);
                engine.switchScene(scene);
            }
        });

        exitButton.addListener(new ChangeListener()
        {
            @Override
            public void changed(ChangeEvent event, Actor actor)
            {
                engine.exit();
            }
        });

        // filling table;
        guiTable.add(title).space(100);
        guiTable.row();
        guiTable.add(playButton).width(100).space(10);
        guiTable.row();
        guiTable.add(exitButton).width(100).space(10);

        AppCloseScript closeScript = new AppCloseScript(Input.Keys.ESCAPE);
        int closeEntity = scene.createEntity();
        scene.addComponent(closeEntity, new ScriptComponent(closeScript));
    }
}
