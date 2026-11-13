package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.Batch;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.EnumMap;
import java.util.Map;
import net.dp.rpg.game.item.PlayerStats;
import net.dp.rpg.game.item.PotionEffect;
import net.dp.rpg.game.item.Stat;
import net.dp.rpg.game.minimap.MinimapIcons;

public class StatBarsWidget extends Table {

  private static final float BAR_WIDTH = 180f;

  private static final float BAR_HEIGHT = 10f;

  // Strength, agility and wisdom reach about 19 with the best gear and a potion; defence caps at
  // 10, where it stops reducing damage (60%).
  private static final Map<Stat, Integer> MAXIMA =
      Map.of(Stat.STRENGTH, 20, Stat.AGILITY, 20, Stat.WISDOM, 20, Stat.DEFENSE, 10);

  private static final Map<Stat, Color> COLOURS = Map.of(
      Stat.STRENGTH, new Color(0.85f, 0.27f, 0.22f, 1f),
      Stat.AGILITY, new Color(0.32f, 0.78f, 0.36f, 1f),
      Stat.WISDOM, new Color(0.27f, 0.52f, 0.95f, 1f),
      Stat.DEFENSE, new Color(0.72f, 0.72f, 0.76f, 1f));

  private final PlayerStats stats;

  private final Map<Stat, Bar> bars = new EnumMap<>(Stat.class);

  private final Map<Stat, Label> values = new EnumMap<>(Stat.class);

  private final Label attack;

  private final Label effects;

  public StatBarsWidget(PlayerStats stats, TextureAtlas atlas, Skin skin) {
    this.stats = stats;
    TextureRegion pixel = atlas.findRegion(MinimapIcons.PIXEL);

    for (Stat stat : Stat.values()) {
      Bar bar = new Bar(pixel, COLOURS.get(stat));
      Label value = new Label("", skin, GameFonts.LABEL);

      bars.put(stat, bar);
      values.put(stat, value);
      add(new Label(stat.shortName(), skin, GameFonts.LABEL)).left().width(44);
      add(bar).size(BAR_WIDTH, BAR_HEIGHT).padRight(8);
      add(value).left().row();
    }

    attack = new Label("", skin, GameFonts.LABEL);
    effects = new Label("", skin, GameFonts.LABEL);
    effects.setColor(0.6f, 0.9f, 1f, 1f);
    add(attack).left().colspan(3).padTop(2).row();
    add(effects).left().colspan(3).row();
  }

  @Override
  public void act(float delta) {
    super.act(delta);

    for (Stat stat : Stat.values()) {
      int value = stats.get(stat);

      bars.get(stat).fraction = Math.min(1f, value / (float) MAXIMA.get(stat));
      values.get(stat).setText(String.valueOf(value));
    }

    attack.setText("ATK %d   Tempo %.2f".formatted(stats.damage(), stats.attackSpeed()));

    StringBuilder text = new StringBuilder();
    for (PlayerStats.ActiveEffect active : stats.effects()) {
      text.append(name(active.effect())).append(' ')
          .append((int) Math.ceil(active.remaining())).append("s  ");
    }
    effects.setText(text.toString().trim());
  }

  private static String name(PotionEffect effect) {
    return switch (effect) {
      case HEAL -> "Leczenie";
      case STRENGTH -> "Siła";
      case WISDOM -> "Inteligencja";
      case SPEED -> "Szybkość";
      case INVISIBILITY -> "Niewidzialność";
    };
  }

  private static final class Bar extends Actor {

    private static final Color BACKGROUND = new Color(0f, 0f, 0f, 0.6f);

    private final TextureRegion pixel;

    private final Color colour;

    private float fraction;

    Bar(TextureRegion pixel, Color colour) {
      this.pixel = pixel;
      this.colour = colour;
    }

    @Override
    public void draw(Batch batch, float parentAlpha) {
      Color previous = batch.getColor().cpy();

      batch.setColor(BACKGROUND.r, BACKGROUND.g, BACKGROUND.b, BACKGROUND.a * parentAlpha);
      batch.draw(pixel, getX(), getY(), getWidth(), getHeight());
      batch.setColor(colour.r, colour.g, colour.b, colour.a * parentAlpha);
      batch.draw(pixel, getX() + 1, getY() + 1, (getWidth() - 2) * fraction, getHeight() - 2);
      batch.setColor(previous);
    }
  }
}
