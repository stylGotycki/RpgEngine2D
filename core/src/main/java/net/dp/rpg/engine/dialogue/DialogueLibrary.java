package net.dp.rpg.engine.dialogue;

import com.badlogic.gdx.utils.JsonReader;
import com.badlogic.gdx.utils.JsonValue;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class DialogueLibrary {

  private final Map<String, Dialogue> dialogues = new LinkedHashMap<>();

  public DialogueLibrary add(String json) {
    for (JsonValue entry : new JsonReader().parse(json).get("dialogues")) {
      Dialogue dialogue = read(entry);

      if (dialogues.put(dialogue.id(), dialogue) != null) {
        throw new IllegalArgumentException("Duplicate dialogue id: " + dialogue.id());
      }
    }

    return this;
  }

  private static Dialogue read(JsonValue entry) {
    Map<String, DialogueNode> nodes = new LinkedHashMap<>();

    for (JsonValue node : entry.get("nodes")) {
      List<DialogueChoice> choices = new ArrayList<>();
      JsonValue choiceList = node.get("choices");

      if (choiceList != null) {
        for (JsonValue choice : choiceList) {
          choices.add(new DialogueChoice(choice.getString("text"),
              choice.getString("next", null), choice.getString("action", null)));
        }
      }

      nodes.put(node.name(), new DialogueNode(node.name(), node.getString("speaker", null),
          node.getString("text", null), node.getString("next", null),
          node.getString("action", null), choices));
    }

    return new Dialogue(entry.getString("id"), entry.getString("start"), nodes);
  }

  public Dialogue get(String id) {
    Dialogue dialogue = dialogues.get(id);

    if (dialogue == null) {
      throw new IllegalArgumentException("Unknown dialogue: " + id);
    }

    return dialogue;
  }

  public boolean has(String id) {
    return dialogues.containsKey(id);
  }

  public List<Dialogue> withPrefix(String prefix) {
    return dialogues.values().stream().filter(dialogue -> dialogue.id().startsWith(prefix))
        .toList();
  }

  public List<Dialogue> all() {
    return List.copyOf(dialogues.values());
  }

  public List<String> problems() {
    List<String> problems = new ArrayList<>();

    dialogues.values().forEach(dialogue -> problems.addAll(dialogue.problems()));

    return problems;
  }
}
