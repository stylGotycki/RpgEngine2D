package net.dp.rpg.engine.interior.prefab;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.interior.corpus.CorpusIssue;
import net.dp.rpg.engine.interior.corpus.CorpusSample;

public final class PrefabLibrary {

  private final Map<String, List<Prefab>> byGroup;

  private PrefabLibrary(Map<String, List<Prefab>> byGroup) {
    this.byGroup = byGroup;
  }

  public static PrefabLibrary extractFrom(List<CorpusSample> samples, List<CorpusIssue> issues) {
    Map<String, List<Prefab>> byGroup = new LinkedHashMap<>();

    for (CorpusSample sample : samples) {
      try {
        for (Prefab prefab : PrefabExtractor.extract(sample, issues)) {
          byGroup.computeIfAbsent(prefab.group(), ignored -> new ArrayList<>()).add(prefab);
        }
      } catch (IllegalArgumentException exception) {
        issues.add(CorpusIssue.error(sample.source(), exception.getMessage()));
      }
    }

    return new PrefabLibrary(byGroup);
  }

  public boolean hasGroup(String group) {
    return byGroup.containsKey(group);
  }

  public List<Prefab> membersOf(String group) {
    List<Prefab> members = byGroup.get(group);

    if (members == null) {
      throw new IllegalArgumentException("No prefab in group '%s'".formatted(group));
    }

    return members;
  }

  public Prefab draw(String group, Random random) {
    List<Prefab> members = membersOf(group);

    if (members.size() == 1) {
      return members.getFirst();
    }

    double total = members.stream().mapToDouble(Prefab::weight).sum();
    double target = random.nextDouble() * total;

    for (Prefab prefab : members) {
      target -= prefab.weight();

      if (target < 0.0) {
        return prefab;
      }
    }

    return members.getLast();
  }
}
