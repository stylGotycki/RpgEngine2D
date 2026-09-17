package net.dp.rpg.engine.tile.tiled;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileLayer;
import net.dp.rpg.engine.tile.TileMapData;
import net.dp.rpg.engine.tile.TileMapObject;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.tile.exception.TilesetCoverageException;
import net.dp.rpg.engine.tile.tileset.TilesetBinding;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

public final class TmxMapWriter {

  private static final int FIRST_GID = 1;

  private static final String LAYER_KIND = "layerKind";

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

    StringBuilder out = new StringBuilder(8192);

    writeHeader(out, map, tileset);
    writeProperties(out, map.properties(), " ");
    writeTilesetReference(out, tileset, settings.targetPath());

    int layerId = 1;

    for (TileLayer layer : map.layers()) {
      writeLayer(out, layer, layerId++, settings);
    }

    writeObjects(out, map, layerId, tileset);

    out.append("</map>").append(System.lineSeparator());

    return out.toString();
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

  private void writeHeader(StringBuilder out, TileMapData map, TilesetDefinition tileset) {
    out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(System.lineSeparator())
        .append("<map version=\"1.10\"").append(System.lineSeparator())
        .append("     tiledversion=\"1.11.2\"").append(System.lineSeparator())
        .append("     orientation=\"orthogonal\"").append(System.lineSeparator())
        .append("     renderorder=\"right-down\"").append(System.lineSeparator())
        .append("     width=\"").append(map.width()).append("\"").append(System.lineSeparator())
        .append("     height=\"").append(map.height()).append("\"").append(System.lineSeparator())
        .append("     tilewidth=\"").append(tileset.tileWidth()).append("\"")
        .append(System.lineSeparator())
        .append("     tileheight=\"").append(tileset.tileHeight()).append("\"")
        .append(System.lineSeparator())
        .append("     infinite=\"0\"").append(System.lineSeparator())
        .append("     nextlayerid=\"").append(map.layers().size() + 2).append("\"")
        .append(System.lineSeparator())
        .append("     nextobjectid=\"").append(map.objects().size() + 1).append("\">")
        .append(System.lineSeparator())
        .append(System.lineSeparator());
  }

  private void writeTilesetReference(StringBuilder out, TilesetDefinition tileset,
                                     String targetPath) {
    String source = TiledPaths.relativize(targetPath, tileset.sourcePath());

    out.append(" <tileset firstgid=\"").append(FIRST_GID).append("\" source=\"")
        .append(escape(source)).append("\"/>")
        .append(System.lineSeparator())
        .append(System.lineSeparator());
  }

  private void writeLayer(StringBuilder out, TileLayer layer, int layerId,
                          TmxExportSettings settings) {
    TileGrid grid = layer.grid();

    out.append(" <layer id=\"").append(layerId)
        .append("\" name=\"").append(escape(layer.name()))
        .append("\" width=\"").append(grid.getWidth())
        .append("\" height=\"").append(grid.getHeight()).append("\">")
        .append(System.lineSeparator());

    // The kind is written back explicitly so a re-import does not have to guess it from the name.
    out.append("  <properties>").append(System.lineSeparator())
        .append("   <property name=\"").append(LAYER_KIND).append("\" value=\"")
        .append(layer.kind()).append("\"/>").append(System.lineSeparator())
        .append("  </properties>").append(System.lineSeparator());

    out.append("  <data encoding=\"csv\">").append(System.lineSeparator());
    writeCsv(out, grid, settings);
    out.append("</data>").append(System.lineSeparator())
        .append(" </layer>").append(System.lineSeparator())
        .append(System.lineSeparator());
  }

  private void writeCsv(StringBuilder out, TileGrid grid, TmxExportSettings settings) {
    TilesetBinding binding = settings.tileset().binding();

    for (int y = 0; y < grid.getHeight(); y++) {
      for (int x = 0; x < grid.getWidth(); x++) {
        out.append(toGid(grid.get(x, y), x, y, binding, settings));

        boolean lastCell = x == grid.getWidth() - 1 && y == grid.getHeight() - 1;

        if (!lastCell) {
          out.append(',');
        }
      }

      out.append(System.lineSeparator());
    }
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

  private void writeObjects(StringBuilder out, TileMapData map, int layerId,
                            TilesetDefinition tileset) {
    if (map.objects().isEmpty()) {
      return;
    }

    out.append(" <objectgroup id=\"").append(layerId).append("\" name=\"Markers\">")
        .append(System.lineSeparator());

    for (TileMapObject object : map.objects()) {
      writeObject(out, object, tileset);
    }

    out.append(" </objectgroup>").append(System.lineSeparator());
  }

  private void writeObject(StringBuilder out, TileMapObject object, TilesetDefinition tileset) {
    out.append("  <object id=\"").append(object.id()).append("\"");

    if (!object.name().isBlank()) {
      out.append(" name=\"").append(escape(object.name())).append("\"");
    }

    if (!object.type().isBlank()) {
      out.append(" type=\"").append(escape(object.type())).append("\"");
    }

    out.append(" x=\"").append(number(object.x() * tileset.tileWidth())).append("\"")
        .append(" y=\"").append(number(object.y() * tileset.tileHeight())).append("\"");

    if (!object.isPoint()) {
      out.append(" width=\"").append(number(object.width() * tileset.tileWidth())).append("\"")
          .append(" height=\"").append(number(object.height() * tileset.tileHeight()))
          .append("\"");
    }

    if (object.properties().isEmpty() && object.isPoint()) {
      out.append("><point/></object>").append(System.lineSeparator());

      return;
    }

    out.append(">").append(System.lineSeparator());
    writeProperties(out, object.properties(), "   ");

    if (object.isPoint()) {
      out.append("   <point/>").append(System.lineSeparator());
    }

    out.append("  </object>").append(System.lineSeparator());
  }

  private void writeProperties(StringBuilder out, Map<String, Object> properties, String indent) {
    if (properties.isEmpty()) {
      return;
    }

    out.append(indent).append("<properties>").append(System.lineSeparator());

    properties.forEach((name, value) -> out.append(indent).append(" <property name=\"")
        .append(escape(name)).append("\"")
        .append(typeAttribute(value))
        .append(" value=\"").append(escape(stringValue(value))).append("\"/>")
        .append(System.lineSeparator()));

    out.append(indent).append("</properties>").append(System.lineSeparator())
        .append(System.lineSeparator());
  }

  private static String typeAttribute(Object value) {
    if (value instanceof Boolean) {
      return " type=\"bool\"";
    }

    if (value instanceof Integer || value instanceof Long) {
      return " type=\"int\"";
    }

    if (value instanceof Number) {
      return " type=\"float\"";
    }

    return "";
  }

  private static String stringValue(Object value) {
    if (value instanceof Float || value instanceof Double) {
      return number(((Number) value).doubleValue());
    }

    return value == null ? "" : value.toString();
  }

  private static String number(double value) {
    if (value == Math.rint(value) && !Double.isInfinite(value)) {
      return Long.toString((long) value);
    }

    return Double.toString(value);
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
