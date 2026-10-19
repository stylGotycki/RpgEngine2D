package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.game.events.HealthChangeEvent;
import net.dp.rpg.game.events.SkillPointUsedEvent;
import net.dp.rpg.game.events.SkillPointsAvailableEvent;
import net.dp.rpg.game.events.SkillWindowToggleEvent;

public class LevelGui
{
    private final Scene scene;
    private final Gui gui;
    private final Table mainTable;

    private final SkillWindow skillWindow;

    public LevelGui(EngineServices engine, Scene scene, int playerId)
    {
        this.scene = scene;
        this.gui = scene.getGui();
        Skin skin = engine.getAssetManager().get("skins/default/default.json");
        skillWindow = new SkillWindow(skin, 4);
        mainTable = gui.getMainTable();
        Stack stack = new Stack();
        mainTable.add(stack).expand().fill();
        Table table = new Table();
        stack.add(table);
        stack.add(skillWindow);
        Engine.eventBus.subscribe(skillWindow, SkillWindowToggleEvent.class);
        Engine.eventBus.subscribe(skillWindow, SkillPointsAvailableEvent.class);
        skillWindow.setVisible(false);
        table.top().left();
        HealthComponent playerHealth = scene.getComponentStorage(HealthComponent.class).getByEntity(playerId);
        HealthBar healthBar = new HealthBar(playerHealth.maxHealth, skin);
        Engine.eventBus.subscribe(healthBar, HealthChangeEvent.class);
        table.add(healthBar).pad(16).width(playerHealth.maxHealth*4);
    }
}
