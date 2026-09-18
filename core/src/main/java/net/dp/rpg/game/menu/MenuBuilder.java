package net.dp.rpg.game.menu;

import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.game.LevelBuilder;
import net.dp.rpg.game.scripts.AppCloseScript;

public class MenuBuilder
{
    public void build(EngineServices engine, Scene scene)
    {
        LevelBuilder levelBuilder = new LevelBuilder();

        Skin guiSkin = engine.getAssetManager().get("skins/default/default.json");
        ParticleEffect buttonParticle = engine.getAssetManager().get("particles/buttonDust.p");
        ParticleEffect backgroundParticle = engine.getAssetManager().get("particles/menuDust.p");

        Gui gui = scene.getGui();
        Table mainTable = gui.getMainTable();
        Stack mainStack = new MenuParticles(backgroundParticle);
        Table guiTable = new Table();

        guiTable.setFillParent(true);

        mainTable.add(mainStack).expand().fill();
        mainStack.add(guiTable);

        // title
        Label title = new Label("THE DUNGEON", guiSkin);

        // buttons
        ParticleButton playButton = new ParticleButton("Play", new ParticleEffect(buttonParticle), guiSkin);
        ParticleButton exitButton = new ParticleButton("Exit", new ParticleEffect(buttonParticle), guiSkin);

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
        Table buttonTable = new Table();
        guiTable.add(title).expandY().pad(90);
        guiTable.row();
        guiTable.add(buttonTable).expandY().top();

        buttonTable.add(playButton).width(200).pad(10);
        buttonTable.row();
        buttonTable.add(exitButton).width(200).pad(10);

        AppCloseScript closeScript = new AppCloseScript(Input.Keys.ESCAPE);
        int closeEntity = scene.createEntity();
        scene.addComponent(closeEntity, new ScriptComponent(closeScript));
    }
}
