package net.dp.rpg.engine.interior.corpus;

public enum Zone {

  VOID,

  WALL,

  DOOR,

  INTERIOR;

  public boolean isInsideRoom() {
    return this != VOID;
  }
}
