package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.events.ItemHoverEvent;
import net.dp.rpg.game.item.ItemDef;
import net.dp.rpg.game.item.ItemDescriptions;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.minimap.MinimapIcons;

public class ItemTooltip extends Table implements GameEventListener {

  private final PlayerStats stats;

  private final Label text;

  private ItemDef item;

  private boolean fromShop;

  public ItemTooltip(PlayerStats stats, TextureAtlas atlas, Skin skin) {
    this.stats = stats;
    this.text = new Label("", skin, GameFonts.LABEL);

    setBackground(new TextureRegionDrawable(atlas.findRegion(MinimapIcons.PIXEL))
        .tint(new Color(0f, 0f, 0f, 0.7f)));
    pad(8, 12, 8, 12);
    add(text).left();
    setVisible(false);
  }

  @Override
  public void onEvent(GameEvent event) {
    if (event instanceof ItemHoverEvent hover) {
      item = hover.item;
      fromShop = hover.fromShop;
    }
  }

  // Rebuilt every frame while shown: requirements and the ATK preview follow the player's stats.
  @Override
  public void act(float delta) {
    super.act(delta);

    setVisible(item != null);
    if (item != null) {
      text.setText(ItemDescriptions.join(ItemDescriptions.describe(item, stats, fromShop)));
      pack();
    }
  }
}
