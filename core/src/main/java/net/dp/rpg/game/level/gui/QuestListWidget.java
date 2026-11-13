package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.Align;
import java.util.List;
import net.dp.rpg.game.quest.QuestTracker;

public class QuestListWidget extends Table {

  private final QuestTracker quests;

  private final Label text;

  private List<String> shown = List.of();

  public QuestListWidget(QuestTracker quests, Skin skin) {
    this.quests = quests;
    this.text = new Label("", skin, GameFonts.LABEL);
    text.setAlignment(Align.right);
    add(text).right();
  }

  @Override
  public void act(float delta) {
    super.act(delta);

    List<String> lines = quests.lines();
    if (!lines.equals(shown)) {
      shown = lines;
      text.setText(String.join("\n", lines));
    }
  }
}
