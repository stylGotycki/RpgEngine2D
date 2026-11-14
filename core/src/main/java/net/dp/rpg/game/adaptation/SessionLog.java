package net.dp.rpg.game.adaptation;

import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.files.FileHandle;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import net.dp.rpg.engine.adaptation.Adaptation;
import net.dp.rpg.engine.adaptation.FloorMetrics;
import net.dp.rpg.engine.adaptation.FloorReport;
import net.dp.rpg.engine.adaptation.PlayStyle;
import net.dp.rpg.engine.adaptation.PlayerProfile;
import net.dp.rpg.engine.quest.GoalType;

// One JSON line per finished floor in sessions/, the raw data for the analysis in the thesis.
public class SessionLog {

  private static final DateTimeFormatter FILE_TIME =
      DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss");

  private final FileHandle file;

  public SessionLog() {
    this.file = Gdx.files == null ? null
        : Gdx.files.local("sessions/session-" + LocalDateTime.now().format(FILE_TIME) + ".jsonl");
  }

  public String path() {
    return file == null ? "-" : file.path();
  }

  public void write(FloorReport report) {
    if (file == null) {
      return;
    }

    try {
      file.writeString(line(report) + "\n", true, "UTF-8");
    } catch (RuntimeException e) {
      // The log is for analysis; a full disk must not end the game.
      Gdx.app.error("Session", "Cannot write " + file.path(), e);
    }
  }

  static String line(FloorReport report) {
    FloorMetrics m = report.metrics();
    Adaptation next = report.next();
    StringBuilder json = new StringBuilder("{");

    field(json, "floor", report.floor());
    field(json, "seed", report.seed());
    json.append("\"metrics\":{");
    field(json, "roomsTotal", m.roomsTotal());
    field(json, "roomsVisited", m.roomsVisited());
    field(json, "enemiesTotal", m.enemiesTotal());
    field(json, "enemiesKilled", m.enemiesKilled());
    field(json, "itemsTotal", m.itemsTotal());
    field(json, "itemsPicked", m.itemsPicked());
    field(json, "puzzlesTotal", m.puzzlesTotal());
    field(json, "puzzlesSolved", m.puzzlesSolved());
    field(json, "talkCorrect", m.talkCorrect());
    field(json, "talkWrong", m.talkWrong());
    field(json, "damageTaken", m.damageTaken());
    field(json, "potionsUsed", m.potionsUsed());
    field(json, "questsOffered", m.questsOffered());
    json.append("\"questsAccepted\":");
    goals(json, m.questsAccepted());
    json.append(',');
    field(json, "questsDeclined", m.questsDeclined());
    field(json, "questsCompleted", m.questsCompleted());
    field(json, "seconds", m.seconds());
    close(json);
    json.append(',');
    profile(json, "measured", report.measured());
    profile(json, "before", report.before());
    profile(json, "after", report.after());
    json.append("\"next\":{");
    field(json, "minRooms", next.floor().minRooms());
    field(json, "maxRooms", next.floor().maxRooms());
    field(json, "trunkRatio", next.floor().trunkRatio());
    field(json, "criticalShare", next.floor().criticalShare());
    field(json, "loopsPerRoom", next.floor().loops().loopsPerRoom());
    field(json, "questRoomRatio", next.floor().questRoomRatio());
    field(json, "difficulty", next.content().difficulty());
    field(json, "potionChance", next.content().potionChance());
    json.append("\"questWeights\":");
    weights(json, next.quests().weights());
    json.append("}}");
    return json.toString();
  }

  private static void profile(StringBuilder json, String name, PlayerProfile profile) {
    json.append('"').append(name).append("\":{");
    for (PlayStyle style : PlayStyle.values()) {
      field(json, style.name(), profile.get(style));
    }
    close(json);
    json.append(',');
  }

  private static void goals(StringBuilder json, Map<GoalType, Integer> counts) {
    json.append('{');
    counts.forEach((type, count) -> field(json, type.name(), count));
    close(json);
  }

  private static void weights(StringBuilder json, Map<GoalType, Double> weights) {
    json.append('{');
    for (GoalType type : GoalType.values()) {
      if (weights.containsKey(type)) {
        field(json, type.name(), weights.get(type));
      }
    }
    close(json);
  }

  private static void field(StringBuilder json, String name, Object value) {
    String text = value instanceof Double || value instanceof Float
        ? String.format(Locale.ROOT, "%.4f", ((Number) value).doubleValue())
        : String.valueOf(value);
    json.append('"').append(name).append("\":").append(text).append(',');
  }

  // Drops the comma after the last field, then closes the object.
  private static void close(StringBuilder json) {
    if (json.charAt(json.length() - 1) == ',') {
      json.setLength(json.length() - 1);
    }
    json.append('}');
  }
}
