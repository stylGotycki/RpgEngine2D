package net.dp.rpg.engine.tile.tiled;

import com.badlogic.gdx.utils.XmlReader.Element;
import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.InvalidTiledFormatException;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;
import net.dp.rpg.engine.tile.tileset.TilesetManager;

public final class TmxMapParser {

  private static final String ROOT_ELEMENT = "map";

  private static final String LAYER_KIND = "layerKind";

  private final TilesetManager tilesetManager;

  private final TileTypeRegistry typeRegistry;

  public TmxMapParser(TilesetManager tilesetManager, TileTypeRegistry typeRegistry) {
    if (tilesetManager == null) {
      throw new IllegalArgumentException("Tileset manager must not be null");
    }

    if (typeRegistry == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    this.tilesetManager = tilesetManager;
    this.typeRegistry = typeRegistry;
  }

  public TileMapData parse(Element root, String sourcePath) {
    if (root == null) {
      throw new InvalidTiledFormatException("Map document must not be null: " + sourcePath);
    }

    if (!ROOT_ELEMENT.equals(root.getName())) {
      throw InvalidTiledFormatException.unexpectedRoot(ROOT_ELEMENT, root.getName(), sourcePath);
    }

    String orientation = root.getAttribute("orientation", "orthogonal");

    if (!"orthogonal".equals(orientation)) {
      throw new InvalidTiledFormatException("Only orthogonal maps are supported, got '%s' in %s"
          .formatted(orientation, sourcePath));
    }

    int width = root.getIntAttribute("width");
    int height = root.getIntAttribute("height");
    int tileWidth = root.getIntAttribute("tilewidth");
    int tileHeight = root.getIntAttribute("tileheight");

    GidResolver resolver = buildResolver(root, sourcePath);

    List<TileLayer> layers = new ArrayList<>();

    for (Element layer : root.getChildrenByName("layer")) {
      layers.add(parseLayer(layer, width, height, resolver, sourcePath));
    }

    if (layers.isEmpty()) {
      throw new InvalidTiledFormatException("Map has no tile layers: " + sourcePath);
    }

    List<TileMapObject> objects = parseObjects(root, tileWidth, tileHeight);

    return new TileMapData(layers, objects, TiledProperties.read(root).asMap());
  }

  private GidResolver buildResolver(Element root, String sourcePath) {
    GidResolver.Builder builder = GidResolver.builder(typeRegistry);

    for (Element reference : root.getChildrenByName("tileset")) {
      int firstGid = reference.getIntAttribute("firstgid");
      String source = reference.getAttribute("source", null);

      if (source == null || source.isBlank()) {
        throw new InvalidTiledFormatException("Embedded tilesets are not supported, use an external TSX in " + sourcePath);
      }

      TilesetDefinition tileset =
          tilesetManager.load(TiledPaths.resolveSibling(sourcePath, source));

      builder.add(firstGid, tileset);
    }

    return builder.build();
  }

  private TileLayer parseLayer(Element source, int mapWidth, int mapHeight, GidResolver resolver,
                               String sourcePath) {
    String name = source.getAttribute("name", "layer");
    int width = source.getIntAttribute("width", mapWidth);
    int height = source.getIntAttribute("height", mapHeight);

    Element data = source.getChildByName("data");

    if (data == null) {
      throw InvalidTiledFormatException.missingElement("data", sourcePath);
    }

    int[] gids = TiledGids.decode(
        data.getAttribute("encoding", null),
        data.getAttribute("compression", null),
        data.getText(),
        width * height);

    TileGrid grid = new TileGrid(width, height);

    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int gid = gids[x + y * width];

        if (gid == 0) {
          continue;
        }

        grid.set(x, y, resolver.resolve(gid, "Layer '%s' at (%d, %d)".formatted(name, x, y)));
      }
    }

    return new TileLayer(name, resolveLayerKind(source, name), grid);
  }

  private TileLayerKind resolveLayerKind(Element source, String name) {
    String declared = TiledProperties.read(source).getString(LAYER_KIND);

    if (declared != null && !declared.isBlank()) {
      return TileLayerKind.from(declared);
    }

    try {
      return TileLayerKind.from(name);
    } catch (RuntimeException exception) {
      return TileLayerKind.GROUND;
    }
  }

  private List<TileMapObject> parseObjects(Element root, int tileWidth, int tileHeight) {
    List<TileMapObject> objects = new ArrayList<>();

    for (Element group : root.getChildrenByName("objectgroup")) {
      for (Element object : group.getChildrenByName("object")) {
        objects.add(parseObject(object, tileWidth, tileHeight));
      }
    }

    return objects;
  }

  private TileMapObject parseObject(Element source, int tileWidth, int tileHeight) {
    return new TileMapObject(
        source.getIntAttribute("id", 0),
        source.getAttribute("name", ""),
        source.getAttribute("type", source.getAttribute("class", "")),
        source.getFloatAttribute("x", 0f) / tileWidth,
        source.getFloatAttribute("y", 0f) / tileHeight,
        source.getFloatAttribute("width", 0f) / tileWidth,
        source.getFloatAttribute("height", 0f) / tileHeight,
        TiledProperties.read(source).asMap());
  }
}
