package net.dp.rpg.engine.content;

/** What stands on a {@link ContentSpot}. The game decides which entity it becomes. */
public enum ContentKind {

  /** A hostile creature; the spot's tag names its type ({@link ContentTags}). */
  ENEMY,

  /** Something to pick up; the tag names the pool it is drawn from, not the item itself. */
  ITEM,

  /** A non-hostile character; blocks its tile, so it never cuts the way between doors. */
  NPC
}
