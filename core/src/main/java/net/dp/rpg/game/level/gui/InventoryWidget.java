package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Container;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Stack;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Align;
import java.util.HashMap;
import java.util.Map;
import net.dp.rpg.game.item.ItemCategory;
import net.dp.rpg.game.item.ItemDef;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.minimap.MinimapIcons;

public class InventoryWidget extends Table {

  private static final float SLOT = 44f;

  private static final ItemCategory[] EQUIPMENT =
      {ItemCategory.WEAPON, ItemCategory.ARMOR, ItemCategory.RING};

  private final PlayerStats stats;

  private final TextureAtlas atlas;

  private final Map<String, Drawable> icons = new HashMap<>();

  private final Image[] equipment = new Image[EQUIPMENT.length];

  private final Image[] potions = new Image[PlayerStats.POTION_SLOTS];

  public InventoryWidget(PlayerStats stats, TextureAtlas atlas, Skin skin) {
    this.stats = stats;
    this.atlas = atlas;

    Drawable background = new TextureRegionDrawable(atlas.findRegion(MinimapIcons.PIXEL))
        .tint(new Color(0f, 0f, 0f, 0.55f));

    for (int i = 0; i < equipment.length; i++) {
      equipment[i] = new Image();
      add(slot(equipment[i], background, null, skin)).size(SLOT).padRight(4);
    }
    add().width(12);
    for (int i = 0; i < potions.length; i++) {
      potions[i] = new Image();
      add(slot(potions[i], background, String.valueOf(i + 1), skin)).size(SLOT).padRight(4);
    }
  }

  private static Stack slot(Image image, Drawable background, String key, Skin skin) {
    Container<Image> frame = new Container<>(image);
    frame.setBackground(background);
    frame.fill().pad(6);

    Stack stack = new Stack(frame);
    if (key != null) {
      Container<Label> label = new Container<>(new Label(key, skin, GameFonts.LABEL));
      label.top().left().padLeft(3);
      stack.add(label);
    }

    return stack;
  }

  @Override
  public void act(float delta) {
    super.act(delta);

    for (int i = 0; i < EQUIPMENT.length; i++) {
      equipment[i].setDrawable(icon(stats.equipped(EQUIPMENT[i])));
    }
    for (int i = 0; i < potions.length; i++) {
      potions[i].setDrawable(icon(stats.potion(i)));
    }
  }

  private Drawable icon(ItemDef item) {
    if (item == null) {
      return null;
    }

    return icons.computeIfAbsent(item.icon(),
        region -> new TextureRegionDrawable(atlas.findRegion(region)));
  }
}
