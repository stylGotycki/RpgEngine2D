package net.dp.rpg.game.victory;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import java.util.List;
import net.dp.rpg.engine.EngineServices;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.adaptation.FloorReport;
import net.dp.rpg.game.adaptation.GameSession;
import net.dp.rpg.game.level.gui.GameFonts;

// The screen after the last floor: one row per floor, the totals, the final profile and where the
// session log is. Reads the finished session; nothing here changes it.
public class VictoryBuilder {

  private static final float PAD = 12;

  public void build(EngineServices engine, Scene scene, GameSession session) {
    Skin skin = engine.getAssetManager().get("skins/default/default.json", Skin.class);
    List<FloorReport> reports = session.adaptive().reports();
    Table content = new Table();

    Label title = label(VictorySummary.TITLE, skin);
    title.setFontScale(2);
    content.add(title).padBottom(24).row();
    content.add(floors(reports, skin)).padBottom(24).row();
    content.add(label(VictorySummary.totalTime(reports), skin)).padBottom(16).row();
    content.add(label(VictorySummary.profile(session.adaptive().profile()), skin))
        .padBottom(16).row();
    content.add(label(VictorySummary.logLine(session.logPath()), skin)).padBottom(24).row();
    content.add(label(VictorySummary.hint(), skin));

    scene.getGui().getMainTable().add(content).expand().center();
    scene.setSceneScript(new VictoryScript());
  }

  private static Table floors(List<FloorReport> reports, Skin skin) {
    Table table = new Table();

    for (String header : VictorySummary.HEADER) {
      table.add(label("[#AAAAAA]" + header + "[]", skin)).padRight(PAD).padBottom(6);
    }
    table.row();

    for (String[] row : VictorySummary.rows(reports)) {
      for (String cell : row) {
        table.add(label(cell, skin)).padRight(PAD).padBottom(4);
      }
      table.row();
    }

    return table;
  }

  private static Label label(String text, Skin skin) {
    return new Label(text, skin, GameFonts.LABEL);
  }
}
