package net.dp.rpg.engine.dialogue;

public record DialogueChoice(String text, String next, String action) {

  public DialogueChoice {
    if (text == null || text.isBlank()) {
      throw new IllegalArgumentException("A choice needs a text");
    }
  }
}
