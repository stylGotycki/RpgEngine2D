package net.dp.rpg.game.level.gui;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Label;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import java.util.List;
import net.dp.rpg.engine.dialogue.DialogueNode;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.minimap.MinimapIcons;

public class DialogueBox extends Table {

  private static final float WIDTH = 720f;

  private static final Color SPEAKER = new Color(0.98f, 0.82f, 0.3f, 1f);

  private static final Color NARRATION = new Color(0.75f, 0.85f, 1f, 1f);

  private static final Color CHOICE = new Color(0.85f, 0.85f, 0.85f, 1f);

  private static final Color SELECTED = new Color(1f, 0.9f, 0.4f, 1f);

  private final DialogueController dialogues;

  private final Label speaker;

  private final Label text;

  private final Label[] choices = new Label[DialogueNode.MAX_CHOICES];

  private final Label hint;

  public DialogueBox(DialogueController dialogues, TextureAtlas atlas, Skin skin) {
    this.dialogues = dialogues;

    setBackground(new TextureRegionDrawable(atlas.findRegion(MinimapIcons.PIXEL))
        .tint(new Color(0.05f, 0.04f, 0.04f, 0.88f)));
    pad(14, 18, 12, 18);
    top().left();

    speaker = new Label("", skin, GameFonts.LABEL);
    speaker.setColor(SPEAKER);
    text = new Label("", skin, GameFonts.LABEL);
    text.setWrap(true);
    hint = new Label("", skin, GameFonts.LABEL);
    hint.setColor(0.6f, 0.6f, 0.6f, 1f);

    add(speaker).left().row();
    add(text).width(WIDTH).left().padTop(4).padBottom(6).row();
    for (int i = 0; i < choices.length; i++) {
      choices[i] = new Label("", skin, GameFonts.LABEL);
      add(choices[i]).left().row();
    }
    add(hint).right().padTop(6);
    setVisible(false);
  }

  @Override
  public void act(float delta) {
    super.act(delta);
    dialogues.update(delta);

    setVisible(dialogues.isOpen());
    if (!dialogues.isOpen()) {
      return;
    }

    String name = dialogues.speaker();
    speaker.setText(name == null ? "" : name);
    speaker.setVisible(name != null);
    text.setText(dialogues.visibleText());
    text.setColor(name == null ? NARRATION : Color.WHITE);

    List<String> options = dialogues.choices();
    for (int i = 0; i < choices.length; i++) {
      boolean shown = i < options.size();
      boolean selected = shown && i == dialogues.selected();

      choices[i].setVisible(shown);
      choices[i].setText(shown ? (selected ? "> " : "  ") + (i + 1) + ". " + options.get(i) : "");
      choices[i].setColor(selected ? SELECTED : CHOICE);
    }

    // "[[" is a literal "[" in the font's colour markup.
    hint.setText(!dialogues.isTextComplete() ? "[[E] pomiń"
        : options.isEmpty() ? "[[E] dalej" : "[[1-3] lub [[W/S] + [[E] wybierz");
  }
}
