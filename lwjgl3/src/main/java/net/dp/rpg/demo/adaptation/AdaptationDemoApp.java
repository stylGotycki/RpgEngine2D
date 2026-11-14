package net.dp.rpg.demo.adaptation;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.graphics.Pixmap;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.InputListener;
import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Slider;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.ScreenUtils;
import com.badlogic.gdx.utils.viewport.FitViewport;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import java.util.EnumMap;
import java.util.Locale;
import java.util.Map;
import net.dp.rpg.demo.floor.DemoFloorPainter;
import net.dp.rpg.demo.floor.FloorReveal;
import net.dp.rpg.engine.adaptation.Adaptation;
import net.dp.rpg.engine.adaptation.AdaptationPolicy;
import net.dp.rpg.engine.adaptation.FloorStatistics;
import net.dp.rpg.engine.adaptation.PlayStyle;
import net.dp.rpg.engine.adaptation.PlayerProfile;
import net.dp.rpg.engine.adaptation.ProfilePreset;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorArchetypes;
import net.dp.rpg.engine.floor.FloorGenerator;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.render.TileMapRenderer;
import net.dp.rpg.engine.tile.room.RoomGeometry;
import net.dp.rpg.game.level.gui.GameFonts;

// Devtool for E6: set the five axes of a player profile by hand and see what the generator makes
// of it. Left: sliders, presets, the policy's decisions and means over many seeds next to the
// neutral profile. Right: the floor of the current seed (TAB: the neutral floor of that seed).
public final class AdaptationDemoApp extends ApplicationAdapter {

  private static final String[] PREVIEW_TILESETS = {"tiles/rooms.tsx"};

  private static final int PANEL_WIDTH = 500;

  private static final int STATS_WIDTH = 400;

  private static final long FIRST_SEED = 20261113L;

  private final AdaptationPolicy policy = new AdaptationPolicy();

  private final FloorArchetype archetype = FloorArchetypes.CATACOMBS;

  private final Map<PlayStyle, Slider> sliders = new EnumMap<>(PlayStyle.class);

  private final Map<PlayStyle, Label> values = new EnumMap<>(PlayStyle.class);

  private final Label[][] statCells = new Label[StatRow.values().length][3];

  private TileSystem previewTiles;

  private DemoFloorPainter painter;

  private TileMapRenderer renderer;

  private OrthographicCamera camera;

  private FitViewport viewport;

  private TileMapData map;

  private Stage stage;

  private BitmapFont font;

  private Texture white;

  private Label title;

  private Label decisions;

  private Label status;

  private SampleWorker worker;

  private Adaptation adaptation;

  private Adaptation neutral;

  private FloorStatistics neutralStats;

  private long seed = FIRST_SEED;

  private boolean showNeutral;

  private boolean applyingPreset;

  @Override
  public void create() {
    previewTiles = new TileSystem(FIRST_SEED);
    previewTiles.loadTilesets(PREVIEW_TILESETS);
    painter = new DemoFloorPainter(previewTiles.types());
    renderer = previewTiles.createRenderer();
    camera = new OrthographicCamera();

    font = new BitmapFont(Gdx.files.internal(GameFonts.FONT));
    font.getData().markupEnabled = true;
    font.setUseIntegerPositions(true);
    Pixmap pixmap = new Pixmap(1, 1, Pixmap.Format.RGBA8888);
    pixmap.setColor(Color.WHITE);
    pixmap.fill();
    white = new Texture(pixmap);
    pixmap.dispose();

    stage = new Stage(new ScreenViewport());
    Gdx.input.setInputProcessor(stage);
    buildPanel();

    neutral = policy.adapt(PlayerProfile.NEUTRAL, archetype);
    worker = new SampleWorker(archetype);
    refresh(true);
  }

  private void buildPanel() {
    Label.LabelStyle labelStyle = new Label.LabelStyle(font, Color.WHITE);
    Slider.SliderStyle sliderStyle = new Slider.SliderStyle(
        box(new Color(0.25f, 0.25f, 0.3f, 1f), 1, 6), box(new Color(0.85f, 0.75f, 0.4f, 1f), 10,
        18));
    TextButton.TextButtonStyle buttonStyle = new TextButton.TextButtonStyle(
        box(new Color(0.2f, 0.2f, 0.25f, 1f), 1, 1), box(new Color(0.4f, 0.4f, 0.5f, 1f), 1, 1),
        null, font);
    buttonStyle.over = box(new Color(0.3f, 0.3f, 0.38f, 1f), 1, 1);

    Table panel = new Table();
    panel.setBackground(box(new Color(0.08f, 0.08f, 0.1f, 0.95f), 1, 1));
    panel.top().left().pad(14);
    panel.defaults().left();

    title = new Label("", labelStyle);
    panel.add(title).colspan(3).padBottom(10).row();

    for (PlayStyle style : PlayStyle.values()) {
      Slider slider = new Slider((float) PlayerProfile.MIN, (float) PlayerProfile.MAX, 0.05f,
          false, sliderStyle);
      Label value = new Label("", labelStyle);
      slider.setValue((float) PlayerProfile.NEUTRAL_VALUE);
      slider.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, Actor actor) {
          if (!applyingPreset) {
            // The preview follows the knob; the means wait until it is let go.
            refresh(!slider.isDragging());
          }
        }
      });
      // Letting go of the knob changes no value, so it gets its own listener.
      slider.addListener(new InputListener() {
        @Override
        public boolean touchDown(InputEvent event, float x, float y, int pointer, int button) {
          return true;
        }

        @Override
        public void touchUp(InputEvent event, float x, float y, int pointer, int button) {
          refresh(true);
        }
      });
      sliders.put(style, slider);
      values.put(style, value);
      panel.add(new Label(style.label(), labelStyle)).width(120);
      panel.add(slider).width(260).padRight(10);
      panel.add(value).width(40).row();
    }

    Table presets = new Table();
    int column = 0;
    for (ProfilePreset preset : ProfilePreset.values()) {
      TextButton button = new TextButton(preset.label(), buttonStyle);
      button.pad(4, 8, 4, 8);
      button.addListener(new ChangeListener() {
        @Override
        public void changed(ChangeEvent event, Actor actor) {
          applyPreset(preset);
        }
      });
      presets.add(button).pad(3).fillX();
      if (++column % 3 == 0) {
        presets.row();
      }
    }
    panel.add(presets).colspan(3).padTop(8).padBottom(10).row();

    decisions = new Label("", labelStyle);
    decisions.setWrap(true);
    panel.add(decisions).colspan(3).width(PANEL_WIDTH - 28).padBottom(10).row();

    Table stats = new Table();
    stats.setBackground(box(new Color(0.08f, 0.08f, 0.1f, 0.95f), 1, 1));
    stats.top().left().pad(14);
    stats.defaults().left().padRight(14);
    stats.add(new Label("[#AAAAAA]średnio[]", labelStyle));
    stats.add(new Label("[#AAAAAA]neutralny[]", labelStyle));
    stats.add(new Label("[#AAAAAA]profil[]", labelStyle));
    stats.add(new Label("", labelStyle)).row();
    for (StatRow row : StatRow.values()) {
      stats.add(new Label(row.label(), labelStyle));
      for (int i = 0; i < 3; i++) {
        statCells[row.ordinal()][i] = new Label("-", labelStyle);
        stats.add(statCells[row.ordinal()][i]);
      }
      stats.row();
    }

    status = new Label("", labelStyle);
    stats.add(status).colspan(4).padTop(10).row();
    panel.add(new Label("[#AAAAAA]R / Shift+R - seed, TAB - piętro neutralne, ESC - wyjście[]",
        labelStyle)).colspan(3).padTop(6);

    Table root = new Table();
    root.setFillParent(true);
    root.add(panel).width(PANEL_WIDTH).expandY().fillY();
    root.add().expand();
    root.add(stats).width(STATS_WIDTH).expandY().fillY();
    stage.addActor(root);
  }

  private Drawable box(Color color, float minWidth, float minHeight) {
    TextureRegionDrawable drawable = new TextureRegionDrawable(new TextureRegion(white));
    drawable.setMinWidth(minWidth);
    drawable.setMinHeight(minHeight);
    return drawable.tint(color);
  }

  private void applyPreset(ProfilePreset preset) {
    applyingPreset = true;
    sliders.forEach((style, slider) -> slider.setValue((float) preset.profile().get(style)));
    applyingPreset = false;
    refresh(true);
  }

  private PlayerProfile profile() {
    PlayerProfile profile = PlayerProfile.NEUTRAL;
    for (Map.Entry<PlayStyle, Slider> entry : sliders.entrySet()) {
      profile = profile.with(entry.getKey(), entry.getValue().getValue());
    }
    return profile;
  }

  private void refresh(boolean sample) {
    PlayerProfile profile = profile();
    adaptation = policy.adapt(profile, archetype);
    values.forEach((style, label) -> label.setText(
        String.format(Locale.ROOT, "%+.2f", profile.get(style))));

    StringBuilder text = new StringBuilder("[#FFD966]Decyzje polityki[]\n");
    adaptation.reasons().forEach(reason -> text.append(reason).append('\n'));
    decisions.setText(text.toString().trim());

    paintPreview();

    if (sample) {
      status.setText("[#AAAAAA]liczę " + SampleWorker.FLOORS + " pięter...[]");
      worker.sample(adaptation, neutralStats == null ? neutral : null, this::showStatistics);
    }
  }

  private void showStatistics(FloorStatistics adapted, FloorStatistics neutralResult) {
    if (neutralResult != null) {
      neutralStats = neutralResult;
    }

    for (StatRow row : StatRow.values()) {
      Label[] cells = statCells[row.ordinal()];
      double base = row.of(neutralStats);
      double value = row.of(adapted);
      double change = base == 0 ? 0 : (value - base) / base * 100;
      cells[0].setText(row.format(base));
      cells[1].setText(row.format(value));
      cells[2].setText(Math.abs(change) < 0.5 ? "[#777777]=[]"
          : String.format(Locale.ROOT, "[#%s]%+.0f%%[]",
              change > 0 ? "8CE68C" : "E07070", change));
    }
    status.setText(String.format(Locale.ROOT, "[#AAAAAA]%d pięter, seedy %d..%d[]",
        adapted.floors(), SampleWorker.FIRST_SEED, SampleWorker.FIRST_SEED + adapted.floors() - 1));
  }

  private void paintPreview() {
    Adaptation shown = showNeutral ? neutral : adaptation;
    FloorLayout layout = new FloorGenerator().generate(archetype, shown.floor(), seed);
    map = painter.paint(layout, layout.extent(), FloorReveal.everything(layout));

    viewport = new FitViewport(RoomGeometry.tileWidth(layout.extent().width()),
        RoomGeometry.tileHeight(layout.extent().height()), camera);
    camera.position.set(map.width() / 2f, map.height() / 2f, 0f);
    updateViewport(Gdx.graphics.getWidth(), Gdx.graphics.getHeight());

    title.setText(String.format(Locale.ROOT,
        "[#FFD966]ADAPTACJA - %s, seed %d[]\npodgląd: %s, %d pokoi, ścieżka krytyczna %d",
        archetype.id(), seed, showNeutral ? "[#88CCFF]neutralny[]" : "ten profil",
        layout.graph().size(), layout.criticalLength()));
  }

  // The map takes the part of the window between the two panels.
  private void updateViewport(int width, int height) {
    viewport.update(Math.max(1, width - PANEL_WIDTH - STATS_WIDTH), height);
    viewport.setScreenX(viewport.getScreenX() + PANEL_WIDTH);
  }

  @Override
  public void render() {
    handleInput();

    ScreenUtils.clear(0.05f, 0.05f, 0.07f, 1f);

    viewport.apply();
    camera.update();
    renderer.render(map, camera);

    stage.getViewport().apply();
    stage.act(Gdx.graphics.getDeltaTime());
    stage.draw();
  }

  private void handleInput() {
    if (Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE)) {
      Gdx.app.exit();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.R)) {
      boolean back = Gdx.input.isKeyPressed(Input.Keys.SHIFT_LEFT)
          || Gdx.input.isKeyPressed(Input.Keys.SHIFT_RIGHT);
      seed += back ? -1 : 1;
      paintPreview();
    }

    if (Gdx.input.isKeyJustPressed(Input.Keys.TAB)) {
      showNeutral = !showNeutral;
      paintPreview();
    }
  }

  @Override
  public void resize(int width, int height) {
    stage.getViewport().update(width, height, true);
    if (viewport != null) {
      updateViewport(width, height);
    }
  }

  @Override
  public void dispose() {
    worker.shutdown();
    stage.dispose();
    font.dispose();
    white.dispose();
    renderer.dispose();
    previewTiles.dispose();
  }

  private enum StatRow {
    ROOMS("Pokoje"),
    CRITICAL("Na ścieżce krytycznej"),
    DEAD_ENDS("Ślepe końce"),
    LOOPS("Pętle"),
    ENEMIES("Wrogowie"),
    PER_ROOM("Wrogów na pokój"),
    FIGHTING("Pokoje z wrogami"),
    MINIBOSSES("Minibossy"),
    SHOPS("Sklepy"),
    PUZZLES("Zagadki"),
    POTIONS("Mikstury"),
    HEALING("w tym leczenia"),
    QUESTS("Questy"),
    FIGHT_QUESTS("  walki"),
    EXPLORE_QUESTS("  eksploracji"),
    THINK_QUESTS("  zagadek/rozmów");

    private final String label;

    StatRow(String label) {
      this.label = label;
    }

    String label() {
      return label;
    }

    String format(double value) {
      return this == CRITICAL || this == FIGHTING
          ? String.format(Locale.ROOT, "%.0f%%", value * 100)
          : String.format(Locale.ROOT, "%.2f", value);
    }

    double of(FloorStatistics s) {
      return switch (this) {
        case ROOMS -> s.rooms();
        case CRITICAL -> s.criticalShare();
        case DEAD_ENDS -> s.deadEnds();
        case LOOPS -> s.loops();
        case ENEMIES -> s.enemies();
        case PER_ROOM -> s.enemiesPerRoom();
        case FIGHTING -> s.fightingShare();
        case MINIBOSSES -> s.minibosses();
        case SHOPS -> s.shops();
        case PUZZLES -> s.puzzles();
        case POTIONS -> s.potions();
        case HEALING -> s.healing();
        case QUESTS -> s.allQuests();
        case FIGHT_QUESTS -> s.quests(GoalType.KILL, GoalType.CHALLENGE);
        case EXPLORE_QUESTS -> s.quests(GoalType.REACH);
        case THINK_QUESTS -> s.quests(GoalType.PUZZLE, GoalType.TALK);
      };
    }
  }
}
