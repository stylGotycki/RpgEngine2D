package net.dp.rpg.game.enemy;

import com.badlogic.gdx.Gdx;

public final class GameEnemies {

  private static EnemyRegistry registry;

  private GameEnemies() {
  }

  public static synchronized EnemyRegistry registry() {
    if (registry == null) {
      registry = EnemyRegistry.parse(Gdx.files.internal(EnemyRegistry.PATH).readString("UTF-8"));
    }

    return registry;
  }
}
