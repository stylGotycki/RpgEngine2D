package net.dp.rpg.engine.floor.io;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import net.dp.rpg.engine.tile.room.RoomCell;

public final class Cells {

  public static final Comparator<RoomCell> READING_ORDER =
      Comparator.comparingInt(RoomCell::y).thenComparingInt(RoomCell::x);

  private Cells() {
  }

  public static String write(RoomCell cell) {
    return cell.x() + "," + cell.y();
  }

  public static RoomCell read(String text) {
    int comma = text.indexOf(',');

    if (comma < 0) {
      throw new IllegalArgumentException("A cell must look like \"x,y\", got: " + text);
    }

    return new RoomCell(Integer.parseInt(text.substring(0, comma).trim()),
        Integer.parseInt(text.substring(comma + 1).trim()));
  }

  public static List<String> write(Iterable<RoomCell> cells) {
    List<RoomCell> sorted = new ArrayList<>();

    cells.forEach(sorted::add);
    sorted.sort(READING_ORDER);

    List<String> written = new ArrayList<>();

    sorted.forEach(cell -> written.add(write(cell)));

    return written;
  }

  public static List<RoomCell> read(List<String> texts) {
    List<RoomCell> cells = new ArrayList<>();

    texts.forEach(text -> cells.add(read(text)));

    return cells;
  }
}