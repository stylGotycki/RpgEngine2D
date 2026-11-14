package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Gui;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.level.events.GameMessageEvent;
import net.dp.rpg.game.level.events.HealthChangeEvent;
import net.dp.rpg.game.level.events.ItemHoverEvent;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.level.events.SkillPointUsedEvent;
import net.dp.rpg.game.level.events.SkillPointsAvailableEvent;
import net.dp.rpg.game.level.events.SkillWindowToggleEvent;
import net.dp.rpg.game.minimap.MinimapWidget;
import net.dp.rpg.game.quest.QuestTracker;

public class LevelGui
{
    private final Scene scene;
    private final Gui gui;
    private final Table mainTable;

    private final SkillWindow skillWindow;

    public LevelGui(EngineServices engine, Scene scene, int playerId, MinimapWidget minimap, PlayerStats stats, TextureAtlas atlas, DialogueController dialogues, QuestTracker quests)
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
        Table minimapTable = new Table();
        minimapTable.top().right();
        minimapTable.add(minimap).pad(16).padBottom(8).size(minimap.getWidth(), minimap.getHeight()).right().row();
        minimapTable.add(new QuestListWidget(quests, skin)).padRight(16).right();
        stack.add(minimapTable);
        Table messageTable = new Table();
        messageTable.bottom();
        MessageLabel messageLabel = new MessageLabel(skin);
        Engine.eventBus.subscribe(messageLabel, GameMessageEvent.class);
        ItemTooltip tooltip = new ItemTooltip(stats, atlas, skin);
        Engine.eventBus.subscribe(tooltip, ItemHoverEvent.class);
        messageTable.add(messageLabel).padBottom(12).row();
        messageTable.add(tooltip).padBottom(40);
        stack.add(messageTable);
        Table dialogueTable = new Table();
        dialogueTable.bottom();
        dialogueTable.add(new DialogueBox(dialogues, atlas, skin)).padBottom(32);
        stack.add(dialogueTable);
        stack.add(skillWindow);
        Engine.eventBus.subscribe(skillWindow, SkillWindowToggleEvent.class);
        Engine.eventBus.subscribe(skillWindow, SkillPointsAvailableEvent.class);
        skillWindow.setVisible(false);
        skillWindow.showLevels(stats);
        table.top().left();
        HealthComponent playerHealth = scene.getComponentStorage(HealthComponent.class).getByEntity(playerId);
        HealthBar healthBar = new HealthBar(playerHealth.maxHealth, skin);
        Engine.eventBus.subscribe(healthBar, HealthChangeEvent.class);
        table.add(healthBar).pad(16).padBottom(8).width(playerHealth.maxHealth*4).left().row();
        table.add(new InventoryWidget(stats, atlas, skin)).padLeft(16).padBottom(8).left().row();
        table.add(new StatBarsWidget(stats, atlas, skin)).padLeft(16).left();
    }
}
