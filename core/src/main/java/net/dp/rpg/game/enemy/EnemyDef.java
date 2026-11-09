package net.dp.rpg.game.enemy;

public record EnemyDef(
    String id,
    String name,
    float health,
    float damage,
    float acceleration,
    float size,
    String sprite,
    float frameTime,
    float spriteScale,
    boolean boss) {

  public EnemyDef {
    if (id == null || id.isBlank()) {
      throw new IllegalArgumentException("An enemy needs an id");
    }
    if (health <= 0 || size <= 0 || acceleration < 0 || damage < 0) {
      throw new IllegalArgumentException("Bad numbers for enemy " + id);
    }
  }
}
