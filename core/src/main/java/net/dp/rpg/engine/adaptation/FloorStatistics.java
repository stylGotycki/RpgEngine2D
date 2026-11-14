package net.dp.rpg.engine.adaptation;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.function.ToDoubleFunction;
import net.dp.rpg.engine.quest.GoalType;

// Means over many floors; one seed says little, the same seeds under two profiles say a lot.
// Shares (critical path, rooms with enemies) are means of each floor's share.
public record FloorStatistics(int floors, double rooms, double criticalLength,
    double criticalShare, double deadEnds, double loops, double enemies, double fightingShare,
    double minibosses, double shops, double puzzles, double potions, double healing,
    Map<GoalType, Double> quests) {

  public FloorStatistics {
    Map<GoalType, Double> copy = new EnumMap<>(GoalType.class);
    copy.putAll(quests);
    quests = Map.copyOf(copy);
  }

  public static FloorStatistics of(List<FloorSample> samples) {
    Map<GoalType, Double> quests = new EnumMap<>(GoalType.class);

    for (GoalType type : GoalType.values()) {
      quests.put(type, mean(samples, sample -> sample.quests().getOrDefault(type, 0)));
    }

    return new FloorStatistics(samples.size(), mean(samples, FloorSample::rooms),
        mean(samples, FloorSample::criticalLength),
        mean(samples, sample -> (double) sample.criticalLength() / sample.rooms()),
        mean(samples, FloorSample::deadEnds), mean(samples, FloorSample::loops),
        mean(samples, FloorSample::enemies),
        mean(samples, sample -> (double) sample.fightingRooms() / sample.rooms()),
        mean(samples, FloorSample::minibosses), mean(samples, FloorSample::shops),
        mean(samples, FloorSample::puzzles), mean(samples, FloorSample::potions),
        mean(samples, FloorSample::healing), quests);
  }

  public double quests(GoalType... types) {
    double total = 0;

    for (GoalType type : types) {
      total += quests.getOrDefault(type, 0.0);
    }

    return total;
  }

  public double allQuests() {
    return quests(GoalType.values());
  }

  public double enemiesPerRoom() {
    return rooms == 0 ? 0 : enemies / rooms;
  }

  private static double mean(List<FloorSample> samples, ToDoubleFunction<FloorSample> value) {
    return samples.stream().mapToDouble(value).average().orElse(0);
  }
}
