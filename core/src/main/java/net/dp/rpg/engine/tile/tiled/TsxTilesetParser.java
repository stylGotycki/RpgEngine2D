package net.dp.rpg.engine.tile.tiled;

import com.badlogic.gdx.utils.XmlReader.Element;
import java.util.ArrayList;
import java.util.List;
import net.dp.rpg.engine.tile.TileLayerKind;
import net.dp.rpg.engine.tile.TileRole;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.exception.InvalidTiledFormatException;
import net.dp.rpg.engine.tile.tileset.LoadedTileset;
import net.dp.rpg.engine.tile.tileset.TilesetBinding;
import net.dp.rpg.engine.tile.tileset.TilesetDefinition;

public final class TsxTilesetParser {

  private static final String ROOT_ELEMENT = "tileset";

  public LoadedTileset parse(Element root, String sourcePath) {
    if (root == null) {
      throw new InvalidTiledFormatException("Tileset document must not be null: " + sourcePath);
    }

    if (!ROOT_ELEMENT.equals(root.getName())) {
      throw InvalidTiledFormatException.unexpectedRoot(ROOT_ELEMENT, root.getName(), sourcePath);
    }

    // The name attribute is the single source of tileset identity; a tilesetId property would be a
    // second one, and the two would eventually disagree.
    String id = requireAttribute(root, "name", sourcePath);
    int tileWidth = root.getIntAttribute("tilewidth");
    int tileHeight = root.getIntAttribute("tileheight");
    int columns = root.getIntAttribute("columns");
    int tileCount = root.getIntAttribute("tilecount");
    int spacing = root.getIntAttribute("spacing", 0);
    int margin = root.getIntAttribute("margin", 0);

    Element image = root.getChildByName("image");

    if (image == null) {
      throw InvalidTiledFormatException.missingElement("image", sourcePath);
    }

    String imagePath = TiledPaths.resolveSibling(
        sourcePath, requireAttribute(image, "source", sourcePath));

    TilesetBinding.Builder binding = TilesetBinding.builder();
    List<TileType> types = new ArrayList<>();

    for (Element tile : root.getChildrenByName("tile")) {
      readTile(tile, sourcePath, binding, types);
    }

    if (types.isEmpty()) {
      throw new InvalidTiledFormatException(
          "Tileset '%s' defines no usable tiles in %s".formatted(id, sourcePath));
    }

    TilesetDefinition definition = new TilesetDefinition(id, sourcePath, imagePath, tileWidth,
        tileHeight, columns, tileCount, spacing, margin, binding.build(),
        TiledProperties.read(root).asMap());

    return new LoadedTileset(definition, types);
  }

  private void readTile(Element tile, String sourcePath, TilesetBinding.Builder binding,
                        List<TileType> types) {
    int localId = tile.getIntAttribute("id");
    TiledProperties properties = TiledProperties.read(tile);

    if (properties.isEmpty()) {
      return;
    }

    String context = "Tile %d in %s".formatted(localId, sourcePath);
    String typeId = properties.requireString(TiledProperties.TILE_ID, context);

    binding.bind(typeId, localId);

    TileType type = TileType.builder()
        .id(typeId)
        .role(TileRole.from(properties.getString(TiledProperties.ROLE)))
        .layer(TileLayerKind.from(properties.getString(TiledProperties.GENERATION_LAYER)))
        .walkable(properties.getBoolean(TiledProperties.WALKABLE))
        .blocksSight(properties.getBoolean(TiledProperties.BLOCKS_SIGHT))
        .defaultWeight(properties.getDouble(TiledProperties.WEIGHT))
        .tags(properties.getTags(TiledProperties.TAGS))
        .properties(properties.asMap())
        .build();

    types.add(type);
  }

  private static String requireAttribute(Element element, String name, String sourcePath) {
    String value = element.getAttribute(name, null);

    if (value == null || value.isBlank()) {
      throw new InvalidTiledFormatException("<%s> has no '%s' attribute in %s"
          .formatted(element.getName(), name, sourcePath));
    }

    return value;
  }
}
