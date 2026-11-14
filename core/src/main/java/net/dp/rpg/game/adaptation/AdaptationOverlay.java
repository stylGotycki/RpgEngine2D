package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.dp.rpg.engine.adaptation.Adaptation;
import net.dp.rpg.engine.adaptation.AdaptiveSession;
import net.dp.rpg.engine.adaptation.FloorMetrics;
import net.dp.rpg.engine.adaptation.PlayStyle;
import net.dp.rpg.engine.adaptation.PlayerProfile;
import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.game.level.gui.GameFonts;
import net.dp.rpg.game.minimap.MinimapIcons;

// F3: the profile this floor was generated from, what the generator made of it, and the profile
// the floor played so far points to. Text with font markup, refreshed a few times a second.
public class AdaptationOverlay extends Table {

  private static final float REFRESH_SECONDS = 0.25f;

  private static final int BAR = 10;

  private final GameSession session;

  private final Label title;

  private final Map<PlayStyle, Label[]> rows = new EnumMap<>(PlayStyle.class);

  private final Label text;

  private float sinceRefresh = REFRESH_SECONDS;

  public AdaptationOverlay(GameSession session, TextureAtlas atlas, Skin skin) {
    this.session = session;
    this.title = new Label("", skin, GameFonts.LABEL);
    this.text = new Label("", skin, GameFonts.LABEL);

    setBackground(new TextureRegionDrawable(atlas.findRegion(MinimapIcons.PIXEL))
        .tint(new Color(0f, 0f, 0f, 0.7f)));
    pad(10);
    defaults().left();
    add(title).row();

    // The font is proportional, so the axes are a table rather than padded text.
    Table profile = new Table();
    profile.defaults().left().padRight(10);
    for (PlayStyle style : PlayStyle.values()) {
      Label[] cells = new Label[4];
      for (int i = 0; i < cells.length; i++) {
        cells[i] = new Label("", skin, GameFonts.LABEL);
        profile.add(cells[i]);
      }
      profile.row();
      rows.put(style, cells);
    }
    add(profile).padTop(4).padBottom(4).row();
    add(text);
    setVisible(false);
  }

  public void toggle() {
    setVisible(!isVisible());
    sinceRefresh = REFRESH_SECONDS;
  }

  @Override
  public void act(float delta) {
    super.act(delta);

    sinceRefresh += delta;
    if (isVisible() && sinceRefresh >= REFRESH_SECONDS) {
      sinceRefresh = 0;
      MetricsCollector collector = session.collector();
      FloorMetrics metrics = collector == null ? null : collector.snapshot();
      PlayerProfile live = metrics == null ? null : session.adaptive().estimate(metrics);
      PlayerProfile current = session.adaptive().current().profile();

      title.setText(heading(session.adaptive()));
      rows.forEach((style, cells) -> {
        String[] values = cells(style, current, live);
        for (int i = 0; i < cells.length; i++) {
          cells[i].setText(values[i]);
        }
      });
      text.setText(describe(session.adaptive(), metrics));
      pack();
    }
  }

  static String heading(AdaptiveSession session) {
    return "[#FFD966]ADAPTACJA - piętro " + session.floor() + "[]\n"
        + "[#AAAAAA]profil, z którego powstało piętro; [#88CCFF]niebieski[] = gra na nim[]";
  }

  // Axis, bar, the value this floor was generated from, the value the floor so far points to.
  static String[] cells(PlayStyle style, PlayerProfile current, PlayerProfile live) {
    double value = current.get(style);
    return new String[] {style.label(), bar(value), String.format(Locale.ROOT, "%.2f", value),
        live == null ? "" : String.format(Locale.ROOT, "[#88CCFF]%.2f[]", live.get(style))};
  }

  static String describe(AdaptiveSession session, FloorMetrics metrics) {
    Adaptation current = session.current();
    StringBuilder out = new StringBuilder();

    out.append("[#FFD966]Generator[]\n");
    out.append(String.format(Locale.ROOT, "pokoje %d-%d, pętle/pokój %.2f, ścieżka %.2f\n",
        current.floor().minRooms(), current.floor().maxRooms(),
        current.floor().loops().loopsPerRoom(), current.floor().criticalShare()));
    out.append(String.format(Locale.ROOT, "trudność x%.2f, mikstury %.0f%%\n",
        current.content().difficulty(), current.content().potionChance() * 100));
    out.append("questy:");
    for (GoalType type : GoalType.values()) {
      double weight = current.quests().of(type);
      if (weight > 0) {
        out.append(String.format(Locale.ROOT, " %s %.1f", type.name(), weight));
      }
    }
    out.append('\n');
    current.reasons().forEach(reason -> out.append("[#AAAAAA]").append(reason).append("[]\n"));

    if (metrics != null) {
      out.append("\n[#FFD966]To piętro[]\n");
      out.append(String.format(Locale.ROOT,
          "pokoje %d/%d, wrogowie %d/%d, przedmioty %d/%d\n", metrics.roomsVisited(),
          metrics.roomsTotal(), metrics.enemiesKilled(), metrics.enemiesTotal(),
          metrics.itemsPicked(), metrics.itemsTotal()));
      out.append(String.format(Locale.ROOT,
          "zagadki %d/%d, rozmowy %d dobrze / %d źle\n", metrics.puzzlesSolved(),
          metrics.puzzlesTotal(), metrics.talkCorrect(), metrics.talkWrong()));
      out.append(String.format(Locale.ROOT,
          "obrażenia %.0f, mikstury %d, questy %d/%d przyjęte, %d ukończone\n",
          metrics.damageTaken(), metrics.potionsUsed(), metrics.accepted(),
          metrics.questsOffered(), metrics.questsCompleted()));
    }

    out.append("\n[#AAAAAA]F5 - następne piętro z adaptacją, F3 - ukryj[]");
    return out.toString();
  }

  private static String bar(double value) {
    int filled = (int) Math.round(value * BAR);
    return "[#77DD77]" + "#".repeat(filled) + "[][#555555]" + "-".repeat(BAR - filled) + "[]";
  }
}
