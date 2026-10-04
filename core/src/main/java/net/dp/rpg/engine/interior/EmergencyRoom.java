package net.dp.rpg.engine.interior;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.floor.RoomBlueprint;
import net.dp.rpg.engine.floor.type.RoomType;
import net.dp.rpg.engine.interior.corpus.Zone;
import net.dp.rpg.engine.interior.model.InteriorModel;
import net.dp.rpg.engine.interior.model.TileClasses;
import net.dp.rpg.engine.interior.pass.RoomCanvas;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.wfc.WeightTable;

/**
 * The room generation falls back to when every attempt fails: the shape's wall band, its doors
 * exactly as the floor graph demands, and one floor material over the whole interior. It carries
 * no fixed element and no details, so a SHOP built this way has no counter; the caller is told
 * through {@link GeneratedRoom#note()}. What it does keep is the one property the floor depends on,
 * that every door opens onto open floor, so the floor graph's connectivity survives a bad room.
 *
 * <p>Wall and floor are the heaviest classes the corpus learned for a NORMAL room, i.e. the
 * material the motif itself calls ordinary, not something chosen here.
 */
final class EmergencyRoom {

  private EmergencyRoom() {
  }

  static TileMapData build(RoomCanvas canvas, InteriorContext context, Random skinRandom) {
    RoomBlueprint blueprint = canvas.blueprint();
    TileClasses classes = context.classes();
    int wall = heaviest(context.model(), Zone.WALL);
    int floor = heaviest(context.model(), Zone.INTERIOR);
    Map<Integer, Integer> doorTokens = doorTokens(canvas, context);
    TileGrid ground = new TileGrid(canvas.width(), canvas.height());
    TileGrid details = new TileGrid(canvas.width(), canvas.height());

    for (int y = 0; y < canvas.height(); y++) {
      for (int x = 0; x < canvas.width(); x++) {
        Zone zone = canvas.zones().zoneAt(x, y);

        if (zone == Zone.VOID) {
          continue;
        }

        int token = switch (zone) {
          case WALL -> wall;
          case DOOR -> doorTokens.get(x + y * canvas.width());
          default -> floor;
        };

        ground.set(x, y, classes.skinOf(token, skinRandom));
      }
    }

    List<TileLayer> layers = List.of(
        new TileLayer("Ground", TileLayerKind.GROUND, ground),
        new TileLayer("Details", TileLayerKind.DETAILS, details));

    return new TileMapData(layers, canvas.objects(), canvas.mapProperties());
  }

  private static Map<Integer, Integer> doorTokens(RoomCanvas canvas, InteriorContext context) {
    Map<Integer, Integer> tokens = new HashMap<>();

    canvas.blueprint().doors().forEach((edge, doorType) -> tokens.put(
        edge.doorTileX() + edge.doorTileY() * canvas.width(), context.doors().tokenOf(doorType)));

    return tokens;
  }

  private static int heaviest(InteriorModel model, Zone zone) {
    WeightTable table = model.weightsOf(RoomType.NORMAL, zone);
    int best = -1;
    double bestWeight = 0.0;

    for (int token = 0; token < table.tokenCount(); token++) {
      if (table.weight(token) > bestWeight) {
        bestWeight = table.weight(token);
        best = token;
      }
    }

    return best;
  }
}
