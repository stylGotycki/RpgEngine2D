package net.dp.rpg.engine.content;

/**
 * One thing in a room, at local tile (x, y) of the room's map (y-up, like the map).
 *
 * @param questId the quest bundle the spot belongs to, or null
 */
public record ContentSpot(ContentKind kind, String tag, String questId, int x, int y) {

  public ContentSpot {
    if (kind == null || tag == null || tag.isBlank()) {
      throw new IllegalArgumentException("A content spot needs a kind and a tag");
    }
  }

  public static ContentSpot of(ContentKind kind, String tag, int x, int y) {
    return new ContentSpot(kind, tag, null, x, y);
  }
}
