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

  public static String relativize(String fromFile, String target) {
    String[] fromSegments = split(parentOf(fromFile));
    String[] targetSegments = split(target.replace('\\', '/'));

    int common = 0;

    while (common < fromSegments.length
        && common < targetSegments.length - 1
        && fromSegments[common].equals(targetSegments[common])) {
      common++;
    }

    StringBuilder builder = new StringBuilder();

    builder.repeat("../", Math.max(0, fromSegments.length - common));

    for (int i = common; i < targetSegments.length; i++) {
      builder.append(targetSegments[i]);

      if (i < targetSegments.length - 1) {
        builder.append('/');
      }
    }

    return builder.isEmpty() ? fileNameOf(target) : builder.toString();
  }

  private static String[] split(String path) {
    return path.isEmpty() ? new String[0] : path.split("/");
  }
}
