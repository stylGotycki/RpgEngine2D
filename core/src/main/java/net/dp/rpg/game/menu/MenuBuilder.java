package net.dp.rpg.game.menu;

import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.scenes.scene2d.ui.*;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;

public class MenuBuilder
{
    public void build(EngineServices engine, Scene scene)
    {
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
        ButtonTable buttonTable = new ButtonTable(engine, guiSkin, buttonParticle);

        // filling table;
        guiTable.add(title).expandY().pad(90);
        guiTable.row();
        guiTable.add(buttonTable).expandY().top();

        scene.setSceneScript(new MenuScript(buttonTable));
    }
}
