package net.dp.rpg.game.item;

import java.util.ArrayList;
import java.util.List;

public final class ItemDescriptions {

  private static final String RED = "[#E85A4F]";

  private static final String GREEN = "[#7FD36B]";

  private static final String GREY = "[#A8A8A8]";

  private static final String END = "[]";

  private ItemDescriptions() {
  }

  public static List<String> describe(ItemDef item, PlayerStats stats, boolean shop) {
    List<String> lines = new ArrayList<>();

    lines.add(item.name());
    lines.add(GREY + category(item) + END);

    if (item.category() == ItemCategory.POTION) {
      lines.add(GREEN + potion(item) + END);
    } else {
      String bonuses = bonuses(item);

      if (!bonuses.isEmpty()) {
        lines.add(GREEN + bonuses + END);
      }
      if (item.attackSpeed() != 1.0) {
        lines.add(GREEN + "Szybkość ataku " + Math.round(item.attackSpeed() * 100) + "%" + END);
      }
      if (item.category() == ItemCategory.WEAPON || item.category() == ItemCategory.RING
          || item.bonus(Stat.STRENGTH) != 0 || item.bonus(Stat.AGILITY) != 0
          || item.bonus(Stat.WISDOM) != 0) {
        int now = stats.damage();
        int then = stats.damageWith(item);
        String colour = then > now ? GREEN : then < now ? RED : GREY;

        lines.add(colour + "ATK " + now + " -> " + then + END);
      }
      if (!item.requires().isEmpty()) {
        lines.add(requirements(item, stats));
      }
    }

    // "[[" is a literal "[" in libGDX colour markup.
    lines.add(shop ? "[[E] Wybierz (zostaje 1 z 3)" : "[[E] Podnieś");

    return lines;
  }

  private static String category(ItemDef item) {
    return switch (item.category()) {
      case WEAPON -> switch (item.weapon()) {
        case SWORD -> "Broń: miecz (siła)";
        case STAFF -> "Broń: kostur (inteligencja)";
        case KNIVES -> "Broń: noże (zręczność)";
        case UNIQUE -> "Broń unikatowa";
        case FISTS -> "Pięści";
      };
      case ARMOR -> "Zbroja";
      case RING -> "Pierścień";
      case POTION -> item.big() ? "Duża mikstura (50 s)" : "Mała mikstura (20 s)";
    };
  }

  private static String potion(ItemDef item) {
    boolean big = item.big();

    return switch (item.potion()) {
      case HEAL -> "+" + (big ? 60 : 25) + " HP od razu";
      case STRENGTH -> "SIL +" + (big ? 4 : 2);
      case WISDOM -> "INT +" + (big ? 4 : 2);
      case SPEED -> "Szybkość +" + (big ? 60 : 30) + "%";
      case INVISIBILITY -> "Wrogowie cię nie widzą";
    };
  }

  private static String bonuses(ItemDef item) {
    StringBuilder text = new StringBuilder();

    for (Stat stat : Stat.values()) {
      int value = item.bonus(stat);

      if (value != 0) {
        text.append(text.isEmpty() ? "" : "  ").append(stat.shortName())
            .append(value > 0 ? " +" : " ").append(value);
      }
    }

    return text.toString();
  }

  private static String requirements(ItemDef item, PlayerStats stats) {
    StringBuilder text = new StringBuilder("Wymaga:");

    for (Stat stat : Stat.values()) {
      Integer required = item.requires().get(stat);

      if (required != null) {
        boolean met = stats.base(stat) >= required;
        text.append(' ').append(met ? GREY : RED).append(stat.shortName()).append(' ')
            .append(required).append(END);
      }
    }

    return text.toString();
  }

  public static String join(List<String> lines) {
    return String.join("\n", lines);
  }
}
