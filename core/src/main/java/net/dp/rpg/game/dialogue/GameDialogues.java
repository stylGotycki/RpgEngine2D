package net.dp.rpg.game.dialogue;

import com.badlogic.gdx.Gdx;
import java.util.List;
import net.dp.rpg.engine.dialogue.DialogueLibrary;

public final class GameDialogues {

  public static final List<String> FILES = List.of(
      "dialogues/narrator.json", "dialogues/quest-givers.json", "dialogues/npcs.json");

  private static DialogueLibrary library;

  private GameDialogues() {
  }

  public static synchronized DialogueLibrary library() {
    if (library == null) {
      DialogueLibrary loaded = new DialogueLibrary();

      for (String file : FILES) {
        loaded.add(Gdx.files.internal(file).readString("UTF-8"));
      }
      if (!loaded.problems().isEmpty()) {
        throw new IllegalStateException("Broken dialogues: " + loaded.problems());
      }
      library = loaded;
    }

    return library;
  }
}
