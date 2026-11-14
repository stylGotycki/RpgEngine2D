package net.dp.rpg.engine.dialogue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class DialogueRunner {

  private static final Pattern VARIABLE = Pattern.compile("\\{(\\w+)\\}");

  private final Dialogue dialogue;

  private final Map<String, String> variables;

  private final Consumer<String> actions;

  private DialogueNode current;

  public DialogueRunner(Dialogue dialogue, Map<String, String> variables,
      Consumer<String> actions) {
    this.dialogue = dialogue;
    this.variables = new HashMap<>(variables);
    this.actions = actions;
    enter(dialogue.start());
  }

  public Dialogue dialogue() {
    return dialogue;
  }

  public boolean isOver() {
    return current == null;
  }

  public DialogueNode current() {
    return current;
  }

  public String speaker() {
    return current == null || current.isNarration() ? null : fill(current.speaker());
  }

  public String text() {
    return current == null ? "" : fill(current.text());
  }

  public List<String> choices() {
    return current == null ? List.of()
        : current.choices().stream().map(choice -> fill(choice.text())).toList();
  }

  public void advance() {
    if (current != null && !current.hasChoices()) {
      enter(current.next());
    }
  }

  public DialogueChoice choose(int index) {
    if (current == null || index < 0 || index >= current.choices().size()) {
      return null;
    }

    DialogueChoice choice = current.choices().get(index);

    if (choice.action() != null) {
      actions.accept(choice.action());
    }
    enter(choice.next());

    return choice;
  }

  private void enter(String nodeId) {
    current = nodeId == null ? null : dialogue.node(nodeId);

    if (current != null && current.action() != null) {
      actions.accept(current.action());
    }
  }

  private String fill(String text) {
    Matcher matcher = VARIABLE.matcher(text);
    StringBuilder filled = new StringBuilder();

    while (matcher.find()) {
      String value = variables.getOrDefault(matcher.group(1), matcher.group(0));
      matcher.appendReplacement(filled, Matcher.quoteReplacement(value));
    }
    matcher.appendTail(filled);

    return filled.toString();
  }
}
