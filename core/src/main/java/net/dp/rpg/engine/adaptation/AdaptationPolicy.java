package net.dp.rpg.engine.adaptation;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.dp.rpg.engine.content.ContentSettings;
import net.dp.rpg.engine.content.ContentSettings.Range;
import net.dp.rpg.engine.floor.BranchSettings;
import net.dp.rpg.engine.floor.FloorArchetype;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.floor.LoopSettings;
import net.dp.rpg.engine.floor.RoomMix;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.engine.quest.QuestWeights;

// Profile (-1..1 per axis) -> generator settings. Every lever is a function of the axes, never of
// a named player type, and each one is the archetype's default at 0: a neutral profile gives
// exactly the designed floor. Multiplicative levers use 2^(sum of weighted axes), so +1 and -1
// on an axis double and halve them, and several axes add up without going negative.
public final class AdaptationPolicy {

  // No quest kind drops below this share of the most wanted one: variety stays.
  public static final double DIVERSITY_FLOOR = 0.2;

  // From this combat value on, every fighting room holds at least one enemy.
  public static final double EVERY_ROOM_FIGHTS = 0.3;

  private static final List<RoomType> FIGHTING_ROOMS = List.of(RoomType.NORMAL,
      RoomType.POWER_FIELD, RoomType.VAULT, RoomType.COLLECTIBLE, RoomType.EMPTY);

  public Adaptation adapt(PlayerProfile profile, FloorArchetype archetype) {
    double c = profile.get(PlayStyle.COMBAT);
    double e = profile.get(PlayStyle.EXPLORATION);
    double k = profile.get(PlayStyle.CAUTION);
    double p = profile.get(PlayStyle.PUZZLES);
    double s = profile.get(PlayStyle.SKILL);
    FloorSettings base = FloorSettings.of(archetype);
    List<String> reasons = new ArrayList<>();

    // Size: explorers and skilled players get more floor, fighters less.
    double scale = clamp(Math.pow(2, 0.75 * e + 0.85 * s - 0.45 * pos(c)), 0.45, 2.3);
    int minRooms = Math.max(8, (int) Math.round(base.minRooms() * scale));
    int maxRooms = Math.max(minRooms, (int) Math.round(base.maxRooms() * scale));
    reasons.add(format("Rozmiar x%.2f: %d-%d pokoi", scale, minRooms, maxRooms));

    // Shape: weak players walk one line to the boss, explorers get long branches.
    double linearity = clamp(1.25 * neg(s) + 0.6 * neg(e) - pos(e), 0, 1);
    double open = 1 - linearity;
    double trunk = clamp(lerp(base.trunkRatio() - 0.3 * pos(e), 1, linearity), 0.3, 1);
    double critical = clamp(lerp(base.criticalShare() - 0.25 * pos(e), 1, linearity), 0.2, 1);
    LoopSettings baseLoops = base.loops();
    LoopSettings loops = new LoopSettings(
        Math.max(0, baseLoops.loopsPerRoom() * (1 + 1.2 * e) * open),
        baseLoops.minCycleRooms(), baseLoops.maxCriticalLoss());
    double chaining = 0.85 * pos(e) * open;
    // Puzzle solvers fight no minibosses at all from 2/3 on the axis.
    double noBosses = clamp(1 - 1.5 * pos(p), 0, 1);
    double deadEnds = clamp((e - 0.3) / 0.5, 0, 1) * noBosses;
    reasons.add(format("Kształt: liniowość %.2f, pień %.2f, ścieżka krytyczna %.2f, "
        + "odnogi w łańcuch %.0f%%, pętle %.2f/pokój", linearity, trunk, critical,
        100 * chaining, loops.loopsPerRoom()));

    RoomMix mix = base.rooms();
    double minibosses = Math.max(0,
        mix.minibosses() + 1.5 * pos(c) + 0.8 * pos(s) - 0.3 * neg(s)) * noBosses;
    double shops = clamp(mix.shops() + 2.2 * pos(e) + 0.6 * pos(s) - 1.2 * pos(p), 0, 4);
    double normal = mix.normal() * Math.pow(2, 0.8 * c);
    double puzzle = mix.puzzle() * Math.pow(2, 1.8 * p - 1.6 * pos(c));
    double empty = mix.empty() * Math.pow(2, -3 * c);
    double collectible = mix.collectible() * Math.pow(2, 1.2 * e - 1.5 * pos(s));
    RoomMix rooms = mix.withShops(shops).withMinibosses(minibosses)
        .withFillers(normal, puzzle, empty, collectible);
    reasons.add(format("Pokoje: sklepy %.1f, minibossy %.1f + %.0f%% ślepych końców, "
        + "zagadki x%.2f, puste x%.2f, ze skarbem x%.2f", shops, minibosses, 100 * deadEnds,
        puzzle / mix.puzzle(), empty / mix.empty(), collectible / mix.collectible()));

    double questRatio = clamp(base.questRoomRatio()
        * Math.pow(2, 1.1 * e + 0.5 * pos(p) - 5 * pos(c - 0.3) - neg(s)), 0, 0.7);
    int bundleMax = (int) clamp(Math.round(base.questBundleMax() + 3.5 * Math.min(0, s)), 1,
        base.questBundleMax());

    FloorSettings floor = base.withRooms(minRooms, maxRooms).withTrunkRatio(trunk)
        .withCriticalShare(critical).withLoops(loops).withQuestRoomRatio(questRatio)
        .withQuestBundleMax(bundleMax).withLinearity(linearity)
        .withBranches(new BranchSettings(chaining, deadEnds)).withRoomMix(rooms);

    ContentSettings defaults = ContentSettings.DEFAULTS;
    double difficulty = clamp(1 + 0.45 * s + 0.35 * c - 0.35 * pos(e), 0.35, 1.9);
    double potions = clamp(defaults.potionChance() * Math.pow(2, 1.2 * c - 1.1 * s + 0.5 * k),
        0.02, 0.6);
    double healing = clamp(0.6 * pos(c) + 0.4 * neg(s), 0, 0.8);
    ContentSettings content = defaults.withDifficulty(difficulty).withPotionChance(potions)
        .withHealingShare(healing);
    if (c >= EVERY_ROOM_FIGHTS) {
      content = content.withEnemies(everyRoomFights(defaults.enemies()));
    }
    reasons.add(format("Wrogowie: trudność x%.2f%s", difficulty,
        c >= EVERY_ROOM_FIGHTS ? ", wróg w każdym pokoju walki" : ""));
    reasons.add(format("Mikstury: %.0f%% pokoi, w tym leczenia %.0f%% + losowe", potions * 100,
        healing * 100));

    QuestWeights quests = questWeights(c, e, p, s);
    reasons.add(format("Questy: %.0f%% wolnych pokoi, do %d pokoi na quest; %s",
        questRatio * 100, bundleMax, describe(quests)));

    return new Adaptation(profile, floor, content, quests, reasons);
  }

  private static Map<RoomType, Range> everyRoomFights(Map<RoomType, Range> enemies) {
    Map<RoomType, Range> result = new EnumMap<>(RoomType.class);
    result.putAll(enemies);

    for (RoomType type : FIGHTING_ROOMS) {
      Range range = enemies.getOrDefault(type, new Range(0, 0));
      int min = Math.max(1, range.min());
      result.put(type, new Range(min, Math.max(min + 1, range.max())));
    }

    return result;
  }

  private static QuestWeights questWeights(double combat, double exploration, double puzzles,
      double skill) {
    QuestWeights defaults = QuestWeights.DEFAULTS;
    Map<GoalType, Double> weights = new EnumMap<>(GoalType.class);

    weights.put(GoalType.KILL, defaults.of(GoalType.KILL) + 0.8 * combat);
    weights.put(GoalType.CHALLENGE,
        defaults.of(GoalType.CHALLENGE) + 0.6 * combat + 0.2 * skill);
    weights.put(GoalType.REACH, defaults.of(GoalType.REACH) + 0.6 * exploration);
    weights.put(GoalType.PUZZLE, defaults.of(GoalType.PUZZLE) + 1.0 * puzzles);
    weights.put(GoalType.TALK, defaults.of(GoalType.TALK) + 0.6 * puzzles);
    weights.put(GoalType.COLLECT, defaults.of(GoalType.COLLECT));
    weights.replaceAll((type, weight) -> Math.max(0, weight));

    double highest = weights.values().stream().mapToDouble(Double::doubleValue).max().orElse(1);
    weights.replaceAll((type, weight) -> defaults.of(type) <= 0 ? 0
        : Math.max(weight, DIVERSITY_FLOOR * highest));

    return new QuestWeights(weights);
  }

  private static String describe(QuestWeights quests) {
    double total = quests.weights().values().stream().mapToDouble(Double::doubleValue).sum();
    StringBuilder text = new StringBuilder();

    for (GoalType type : GoalType.values()) {
      if (quests.of(type) > 0) {
        text.append(text.isEmpty() ? "" : ", ").append(type).append(' ')
            .append(Math.round(100 * quests.of(type) / total)).append('%');
      }
    }

    return text.toString();
  }

  private static String format(String pattern, Object... values) {
    return String.format(Locale.ROOT, pattern, values);
  }

  private static double pos(double value) {
    return Math.max(0, value);
  }

  private static double neg(double value) {
    return Math.max(0, -value);
  }

  private static double lerp(double from, double to, double amount) {
    return from + (to - from) * amount;
  }

  private static double clamp(double value, double min, double max) {
    return Math.max(min, Math.min(max, value));
  }
}
