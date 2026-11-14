package net.dp.rpg.engine.dialogue;

import java.util.List;

public record DialogueNode(String id, String speaker, String text, String next, String action,
    List<DialogueChoice> choices) {

  public static final int MAX_CHOICES = 3;

  public DialogueNode {
    choices = choices == null ? List.of() : List.copyOf(choices);
  }

  public boolean isNarration() {
    return speaker == null || speaker.isBlank();
  }

  public boolean hasChoices() {
    return !choices.isEmpty();
  }
}
