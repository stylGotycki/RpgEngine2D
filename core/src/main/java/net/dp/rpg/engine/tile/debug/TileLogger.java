package net.dp.rpg.engine.tile.debug;

@FunctionalInterface
public interface TileLogger {

  void log(String message);

  TileLogger SILENT = message -> {
  };

  TileLogger CONSOLE = System.out::println;
}
