package net.dp.rpg.game.level;

import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.HealthComponent;
import net.dp.rpg.engine.components.ScriptComponent;
import net.dp.rpg.game.scripts.PlayerScript;

/**
 * What the player brings to the next floor besides {@code PlayerStats} (stats, items, potions and
 * their timers already live in that one object): current health and unspent skill points.
 *
 * @param health current health, or a value below 1 for "start at full health"
 * @param skillPoints skill points not yet spent
 */
public record PlayerCarry(float health, int skillPoints) {

  /** A new game: full health, no points. */
  public static final PlayerCarry NONE = new PlayerCarry(0, 0);

  public static PlayerCarry capture(Scene scene, int player) {
    HealthComponent health = scene.getComponentStorage(HealthComponent.class).getByEntity(player);
    ScriptComponent script = scene.getComponentStorage(ScriptComponent.class).getByEntity(player);
    int points = script != null && script.script instanceof PlayerScript playerScript
        ? playerScript.getAvailableSkillPoints() : 0;

    return new PlayerCarry(health == null ? 0 : health.health, points);
  }

  public boolean hasHealth() {
    return health >= 1;
  }

  // Never above the new body's maximum, so a larger maximum of an earlier floor cannot leak in.
  public float healthFor(float maxHealth) {
    return hasHealth() ? Math.min(health, maxHealth) : maxHealth;
  }
}
