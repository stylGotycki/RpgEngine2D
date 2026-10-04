package net.dp.rpg.engine.interior.corpus;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.function.Function;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.exception.InvalidCorpusException;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileTypeRegistry;

public final class RoomCorpus {

  public static final String ROOMS_DIRECTORY = "rooms/";

  private static final String MAP_SUFFIX = ".tmx";

  private final String motif;

  private final List<CorpusSample> samples;

  private final List<CorpusIssue> warnings;

  private RoomCorpus(String motif, List<CorpusSample> samples, List<CorpusIssue> warnings) {
    this.motif = motif;
    this.samples = List.copyOf(samples);
    this.warnings = List.copyOf(warnings);
  }

  public static List<String> discover(String motif, Collection<String> assetPaths) {
    String prefix = ROOMS_DIRECTORY + requireMotif(motif) + "/";

    return assetPaths.stream()
        .map(path -> path.trim().replace('\\', '/'))
        .filter(path -> path.startsWith(prefix) && path.endsWith(MAP_SUFFIX))
        .distinct()
        .sorted()
        .toList();
  }

  public static RoomCorpus load(String motif, Collection<String> assetPaths,
                                Function<String, TileMapData> loader, TileTypeRegistry types) {
    List<String> paths = discover(motif, assetPaths);
    List<CorpusIssue> issues = new ArrayList<>();
    List<CorpusSample> samples = new ArrayList<>();
    CorpusScanner scanner = new CorpusScanner(types);

    if (paths.isEmpty()) {
      issues.add(CorpusIssue.error(ROOMS_DIRECTORY + motif,
          "No " + MAP_SUFFIX + " samples found for this motif"));
    }

    for (String path : paths) {
      TileMapData map;

      try {
        map = loader.apply(path);
      } catch (RuntimeException exception) {
        issues.add(CorpusIssue.error(path, "Cannot load: %s: %s".formatted(
            exception.getClass().getSimpleName(), exception.getMessage())));
        continue;
      }

      CorpusScanner.ScanResult result = scanner.scan(path, map);

      issues.addAll(result.issues());

      if (result.accepted()) {
        samples.add(result.sample());
      }
    }

    if (issues.stream().anyMatch(CorpusIssue::isError)) {
      throw new InvalidCorpusException(motif, issues);
    }

    return new RoomCorpus(motif, samples, issues);
  }

  public String motif() {
    return motif;
  }

  public List<CorpusSample> samples() {
    return samples;
  }

  public List<CorpusSample> samplesFor(RoomType type) {
    return samples.stream().filter(sample -> sample.appliesTo(type)).toList();
  }

  public List<CorpusIssue> warnings() {
    return warnings;
  }

  private static String requireMotif(String motif) {
    if (motif == null || motif.isBlank() || motif.contains("/") || motif.contains("\\")) {
      throw new IllegalArgumentException("Motif must be a single directory name: " + motif);
    }

    return motif;
  }
}
