package net.dp.rpg.engine.adaptation;

public enum PlayStyle {
  COMBAT("Walka"),
  EXPLORATION("Eksploracja"),
  CAUTION("Ostrożność"),
  PUZZLES("Zagadki"),
  SKILL("Umiejętność");

  private final String label;

  PlayStyle(String label) {
    this.label = label;
  }

  public String label() {
    return label;
  }
}
