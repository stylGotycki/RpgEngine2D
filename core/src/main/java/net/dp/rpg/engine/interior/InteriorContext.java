package net.dp.rpg.engine.interior;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import net.dp.rpg.engine.floor.graph.DoorType;
import net.dp.rpg.engine.interior.corpus.CorpusIssue;
import net.dp.rpg.engine.interior.corpus.RoomCorpus;
import net.dp.rpg.engine.interior.detail.DetailLearner;
import net.dp.rpg.engine.interior.detail.DetailStatistics;
import net.dp.rpg.engine.interior.exception.InvalidCorpusException;
import net.dp.rpg.engine.interior.model.AdjacencyLearner;
import net.dp.rpg.engine.interior.model.DoorPalette;
import net.dp.rpg.engine.interior.model.InteriorModel;
import net.dp.rpg.engine.interior.model.TileClasses;
import net.dp.rpg.engine.interior.model.WalkableClasses;
import net.dp.rpg.engine.interior.prefab.PrefabLibrary;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileTypeRegistry;

public record InteriorContext(
    RoomCorpus corpus,
    TileClasses classes,
    InteriorModel model,
    DoorPalette doors,
    WalkableClasses walkable,
    PrefabLibrary prefabs,
    Map<Integer, DetailStatistics> details) {

  public static InteriorContext load(String motif, Collection<String> assetPaths,
      Function<String, TileMapData> loader, TileTypeRegistry types,
      Map<String, List<String>> classGroups, Map<DoorType, String> doorClasses) {
    RoomCorpus corpus = RoomCorpus.load(motif, assetPaths, loader, types);
    TileClasses classes = TileClasses.of(types, classGroups);
    InteriorModel model = new AdjacencyLearner(classes).learn(corpus.samples());
    DoorPalette doors = DoorPalette.of(classes, doorClasses);
    WalkableClasses walkable = WalkableClasses.of(classes, types);
    List<CorpusIssue> prefabIssues = new ArrayList<>();
    PrefabLibrary prefabs = PrefabLibrary.extractFrom(corpus.samples(), prefabIssues);

    if (prefabIssues.stream().anyMatch(CorpusIssue::isError)) {
      throw new InvalidCorpusException(motif, prefabIssues);
    }

    Map<Integer, DetailStatistics> details = DetailLearner.learn(corpus.samples(), classes);

    return new InteriorContext(corpus, classes, model, doors, walkable, prefabs, details);
  }
}
