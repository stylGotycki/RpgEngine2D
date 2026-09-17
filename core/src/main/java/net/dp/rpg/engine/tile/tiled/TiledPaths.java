package net.dp.rpg.engine.tile.tiled;

import java.util.ArrayDeque;
import java.util.Deque;

public final class TiledPaths {

  private TiledPaths() {
  }

  public static String resolveSibling(String sourcePath, String relative) {
    if (relative == null || relative.isBlank()) {
      throw new IllegalArgumentException("Relative path must not be blank");
    }

    String normalized = relative.replace('\\', '/');

    if (normalized.startsWith("/")) {
      return normalize(normalized);
    }

    return normalize(parentOf(sourcePath) + "/" + normalized);
  }

  public static String parentOf(String path) {
    String normalized = path.replace('\\', '/');
    int lastSlash = normalized.lastIndexOf('/');

    return lastSlash < 0 ? "" : normalized.substring(0, lastSlash);
  }

  public static String fileNameOf(String path) {
    String normalized = path.replace('\\', '/');

    return normalized.substring(normalized.lastIndexOf('/') + 1);
  }

  private static String normalize(String path) {
    Deque<String> segments = new ArrayDeque<>();

    for (String segment : path.split("/")) {
      if (segment.isEmpty() || ".".equals(segment)) {
        continue;
      }

      if ("..".equals(segment) && !segments.isEmpty() && !"..".equals(segments.peekLast())) {
        segments.removeLast();
        continue;
      }

      segments.addLast(segment);
    }

    return String.join("/", segments);
  }
}
