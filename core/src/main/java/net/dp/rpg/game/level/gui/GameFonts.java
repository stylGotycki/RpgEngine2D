package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;

public final class GameFonts {

  public static final String FONT = "fonts/minecraft.fnt";

  public static final String LABEL = "minecraft";

  private GameFonts() {
  }

  public static void register(Skin skin, BitmapFont font) {
    font.getData().markupEnabled = true;
    font.setUseIntegerPositions(true);
    skin.add(LABEL, font, BitmapFont.class);
    skin.add(LABEL, new Label.LabelStyle(font, Color.WHITE), Label.LabelStyle.class);
  }
}
