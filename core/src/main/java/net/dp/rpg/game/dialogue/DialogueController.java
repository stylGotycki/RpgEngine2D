package net.dp.rpg.game.dialogue;

import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.dialogue.Dialogue;
import net.dp.rpg.engine.dialogue.DialogueChoice;
import net.dp.rpg.engine.dialogue.DialogueNode;
import net.dp.rpg.engine.dialogue.DialogueRunner;
import net.dp.rpg.game.events.DialogueChoiceEvent;
import net.dp.rpg.game.events.DialogueEndedEvent;

public class DialogueController {

  private static final float CHARACTERS_PER_SECOND = 45f;

  private DialogueRunner runner;

  private float revealed;

  private int selected;

  public void open(Dialogue dialogue, Map<String, String> variables, Consumer<String> actions) {
    runner = new DialogueRunner(dialogue, variables, actions);
    restartNode();
  }

  public boolean isOpen() {
    return runner != null && !runner.isOver();
  }

  public void update(float delta) {
    if (isOpen()) {
      revealed = Math.min(runner.text().length(), revealed + delta * CHARACTERS_PER_SECOND);
    }
  }

  public String speaker() {
    return isOpen() ? runner.speaker() : null;
  }

  public String visibleText() {
    return isOpen() ? runner.text().substring(0, (int) revealed) : "";
  }

  public boolean isTextComplete() {
    return isOpen() && (int) revealed >= runner.text().length();
  }

  public List<String> choices() {
    return isOpen() && isTextComplete() ? runner.choices() : List.of();
  }

  public int selected() {
    return selected;
  }

  public void moveSelection(int step) {
    int count = choices().size();

    if (count > 0) {
      selected = Math.floorMod(selected + step, count);
    }
  }

  // E or space: finish the line first, then go on (or take the highlighted choice).
  public void confirm() {
    if (!isOpen()) {
      return;
    }
    if (!isTextComplete()) {
      revealed = runner.text().length();
    } else if (runner.current().hasChoices()) {
      choose(selected);
    } else {
      step(runner::advance);
    }
  }

  public void choose(int index) {
    if (!isOpen() || !isTextComplete() || index >= runner.current().choices().size()) {
      return;
    }

    DialogueNode node = runner.current();
    String dialogueId = runner.dialogue().id();
    step(() -> {
      DialogueChoice choice = runner.choose(index);

      Engine.eventBus.publish(DialogueChoiceEvent.class, e -> {
        e.dialogueId = dialogueId;
        e.nodeId = node.id();
        e.choice = index;
        e.action = choice.action();
      });
    });
  }

  private void step(Runnable move) {
    String dialogueId = runner.dialogue().id();

    move.run();
    if (runner.isOver()) {
      runner = null;
      Engine.eventBus.publish(DialogueEndedEvent.class, e -> e.dialogueId = dialogueId);
    } else {
      restartNode();
    }
  }

  private void restartNode() {
    revealed = 0;
    selected = 0;
  }
}
