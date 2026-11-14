package net.dp.rpg.game.enemy;

public final class PolishPlural {

  private PolishPlural() {
  }

  // 1 pająk, 2-4 pająki (but 12-14 pająków), 5+ pająków.
  public static String choose(int count, String one, String few, String many) {
    int lastTwo = Math.abs(count) % 100;
    int last = lastTwo % 10;

    if (count == 1) {
      return one;
    }
    if (last >= 2 && last <= 4 && (lastTwo < 12 || lastTwo > 14)) {
      return few;
    }

    return many;
  }
}
