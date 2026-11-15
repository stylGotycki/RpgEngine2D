package net.dp.rpg.game.victory;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.dp.rpg.engine.adaptation.FloorMetrics;
import net.dp.rpg.engine.adaptation.FloorReport;
import net.dp.rpg.engine.adaptation.PlayStyle;
import net.dp.rpg.engine.adaptation.PlayerProfile;

// The text of the victory screen, kept apart from the widgets so it can be tested: every glyph
// must exist in the game's bitmap font, which has no fallback.
public final class VictorySummary {

  public static final String TITLE = "[#FFD966]ZWYCIĘSTWO[]";

  public static final String[] HEADER = {"Piętro", "Pokoje", "Wrogowie", "Zagadki", "Questy",
      "Obrażenia", "Czas"};

  private VictorySummary() {
  }

  // One row per floor, then a row of totals. Counts are "done/available".
  public static List<String[]> rows(List<FloorReport> reports) {
    List<String[]> rows = new ArrayList<>();
    int[] totals = new int[8];
    float damage = 0;
    double seconds = 0;

    for (FloorReport report : reports) {
      FloorMetrics m = report.metrics();
      int[] values = {m.roomsVisited(), m.roomsTotal(), m.enemiesKilled(), m.enemiesTotal(),
          m.puzzlesSolved(), m.puzzlesTotal(), m.questsCompleted(), m.questsOffered()};

      for (int i = 0; i < values.length; i++) {
        totals[i] += values[i];
      }
      damage += m.damageTaken();
      seconds += m.seconds();
      rows.add(row(String.valueOf(report.floor()), values, m.damageTaken(), m.seconds()));
    }

    rows.add(row("[#FFD966]Razem[]", totals, damage, seconds));
    return rows;
  }

  private static String[] row(String name, int[] v, float damage, double seconds) {
    return new String[] {name, ratio(v[0], v[1]), ratio(v[2], v[3]), ratio(v[4], v[5]),
        ratio(v[6], v[7]), String.format(Locale.ROOT, "%.0f", damage), time(seconds)};
  }

  // A count above its total (enemies come back for a challenge) still reads as complete.
  static String ratio(int done, int total) {
    return Math.min(done, total) + "/" + total;
  }

  public static String time(double seconds) {
    long whole = Math.round(seconds);

    return whole >= 3600
        ? String.format(Locale.ROOT, "%d:%02d:%02d", whole / 3600, whole / 60 % 60, whole % 60)
        : String.format(Locale.ROOT, "%d:%02d", whole / 60, whole % 60);
  }

  public static double totalSeconds(List<FloorReport> reports) {
    return reports.stream().mapToDouble(report -> report.metrics().seconds()).sum();
  }

  public static String totalTime(List<FloorReport> reports) {
    return "Łączny czas gry: " + time(totalSeconds(reports));
  }

  // The profile the game ended with: what the adaptation took the player for.
  public static String profile(PlayerProfile profile) {
    StringBuilder text = new StringBuilder("[#FFD966]Twój styl gry[]");

    for (PlayStyle style : PlayStyle.values()) {
      text.append('\n').append(style.label()).append(": ")
          .append(String.format(Locale.ROOT, "%+.2f", profile.get(style)));
    }

    return text.toString();
  }

  public static String logLine(String path) {
    return "[#AAAAAA]Log sesji: " + path + "[]";
  }

  public static String hint() {
    return "[#AAAAAA]Enter - wróć do menu[]";
  }

  // Everything the screen shows, as plain lines, for checking glyphs.
  public static List<String> allText(List<FloorReport> reports, PlayerProfile profile,
      String logPath) {
    List<String> lines = new ArrayList<>();

    lines.add(TITLE);
    lines.addAll(List.of(HEADER));
    rows(reports).forEach(row -> lines.addAll(List.of(row)));
    lines.add(totalTime(reports));
    lines.add(profile(profile));
    lines.add(logLine(logPath));
    lines.add(hint());
    return lines;
  }
}
