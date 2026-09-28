package net.dp.rpg.engine.floor.graph;

import net.dp.rpg.engine.tile.room.RoomEdge;

public record RoomLink(RoomNode from, RoomNode to, RoomEdge edge, DoorType doorType) {

  public RoomNode other(RoomNode node) {
    return node == from ? to : from;
  }

  public boolean joins(RoomNode first, RoomNode second) {
    return (from == first && to == second) || (from == second && to == first);
  }
}
