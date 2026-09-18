package net.dp.rpg.engine.tile.tiled;

import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.TilesetCoverageException;
import net.dp.rpg.engine.tile.tileset.TilesetBinding;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class TmxMapWriter {

  private static final int FIRST_GID = 1;

  private static final String LAYER_KIND = "layerKind";

  private static final String TILED_VERSION = "1.11.2";

  private static final String MAP_VERSION = "1.10";

  private final TileTypeRegistry typeRegistry;

  public TmxMapWriter(TileTypeRegistry typeRegistry) {
    if (typeRegistry == null) {
      throw new IllegalArgumentException("Tile type registry must not be null");
    }

    this.typeRegistry = typeRegistry;
  }

  public String write(TileMapData map, TmxExportSettings settings) {
    if (map == null) {
      throw new IllegalArgumentException("Tile map must not be null");
    }

    TilesetDefinition tileset = settings.tileset();

    validateCoverage(map, tileset);

    XmlWriter xml = new XmlWriter().declaration();

    writeMapElement(xml, map, tileset);
    writeProperties(xml, map.properties());
    writeTilesetReference(xml, tileset, settings.targetPath());

    int layerId = 1;

    for (TileLayer layer : map.layers()) {
      writeLayer(xml, layer, layerId++, settings);
    }

    writeObjects(xml, map, layerId, tileset);

    xml.close();

    return xml.toString();
  }

  private void validateCoverage(TileMapData map, TilesetDefinition tileset) {
    List<String> missing = new ArrayList<>();

    for (int runtimeId : map.usedRuntimeIds()) {
      String typeId = typeRegistry.require(runtimeId).id();

      if (!tileset.binding().covers(typeId)) {
        missing.add(typeId);
      }
    }

    if (!missing.isEmpty()) {
      throw new TilesetCoverageException(tileset.id(), missing);
    }
  }

  private void writeMapElement(XmlWriter xml, TileMapData map, TilesetDefinition tileset) {
    xml.open("map")
        .attr("version", MAP_VERSION)
        .attr("tiledversion", TILED_VERSION)
        .attr("orientation", "orthogonal")
        .attr("renderorder", "right-down")
        .attr("width", map.width())
        .attr("height", map.height())
        .attr("tilewidth", tileset.tileWidth())
        .attr("tileheight", tileset.tileHeight())
        .attr("infinite", 0)
        .attr("nextlayerid", map.layers().size() + 2)
        .attr("nextobjectid", map.objects().size() + 1);
  }

  private void writeTilesetReference(XmlWriter xml, TilesetDefinition tileset, String targetPath) {
    xml.open("tileset")
        .attr("firstgid", FIRST_GID)
        .attr("source", TiledPaths.relativize(targetPath, tileset.sourcePath()))
        .close()
        .blankLine();
  }

  private void writeLayer(XmlWriter xml, TileLayer layer, int layerId,
                          TmxExportSettings settings) {
    TileGrid grid = layer.grid();

    xml.open("layer")
        .attr("id", layerId)
        .attr("name", layer.name())
        .attr("width", grid.getWidth())
        .attr("height", grid.getHeight());

    xml.open("properties")
        .open("property")
        .attr("name", LAYER_KIND)
        .attr("value", layer.kind().name())
        .close()
        .close();

    xml.open("data")
        .attr("encoding", "csv")
        .textBlock(toCsv(grid, settings))
        .close();

    xml.close().blankLine();
  }

  private String toCsv(TileGrid grid, TmxExportSettings settings) {
    TilesetBinding binding = settings.tileset().binding();
    StringBuilder csv = new StringBuilder(grid.size() * 4);

    for (int y = 0; y < grid.getHeight(); y++) {
      for (int x = 0; x < grid.getWidth(); x++) {
        csv.append(toGid(grid.get(x, y), x, y, binding, settings));

        boolean lastCell = x == grid.getWidth() - 1 && y == grid.getHeight() - 1;

        if (!lastCell) {
          csv.append(',');
        }
      }

      csv.append(System.lineSeparator());
    }

    return csv.toString();
  }

  private int toGid(int runtimeId, int x, int y, TilesetBinding binding,
                    TmxExportSettings settings) {
    if (runtimeId == TileGrid.EMPTY) {
      return 0;
    }

    String typeId = typeRegistry.require(runtimeId).id();

    int localId = settings.usesVariants()
        ? binding.localId(typeId, x, y, settings.variantSeed())
        : binding.localId(typeId);

    return FIRST_GID + localId;
  }

  private void writeObjects(XmlWriter xml, TileMapData map, int layerId,
                            TilesetDefinition tileset) {
    if (map.objects().isEmpty()) {
      return;
    }

    xml.open("objectgroup")
        .attr("id", layerId)
        .attr("name", "Markers");

    for (TileMapObject object : map.objects()) {
      writeObject(xml, object, tileset);
    }

    xml.close();
  }

  private void writeObject(XmlWriter xml, TileMapObject object, TilesetDefinition tileset) {
    // Objects are kept in tile units internally and converted back to pixels only here, which is
    // what lets a map survive a tileset with a different tile size.
    xml.open("object")
        .attr("id", object.id())
        .attrIfPresent("name", object.name())
        .attrIfPresent("type", object.type())
        .attr("x", object.x() * tileset.tileWidth())
        .attr("y", object.y() * tileset.tileHeight());

    if (!object.isPoint()) {
      xml.attr("width", object.width() * tileset.tileWidth())
          .attr("height", object.height() * tileset.tileHeight());
    }

    writeProperties(xml, object.properties());

    if (object.isPoint()) {
      xml.open("point").close();
    }

    xml.close();
  }

  private void writeProperties(XmlWriter xml, Map<String, Object> properties) {
    if (properties.isEmpty()) {
      return;
    }

    xml.open("properties");

    properties.forEach((name, value) -> {
      xml.open("property").attr("name", name);

      String type = typeOf(value);

      if (type != null) {
        xml.attr("type", type);
      }

      xml.attr("value", stringValue(value)).close();
    });

    xml.close();
  }

  private static String typeOf(Object value) {
    if (value instanceof Boolean) {
      return "bool";
    }

    if (value instanceof Integer || value instanceof Long) {
      return "int";
    }

    if (value instanceof Number) {
      return "float";
    }

    return null;
  }

  private static String stringValue(Object value) {
    if (value instanceof Float || value instanceof Double) {
      double number = ((Number) value).doubleValue();

      return number == Math.rint(number) ? Long.toString((long) number) : Double.toString(number);
    }

    return value == null ? "" : value.toString();
  }
}
