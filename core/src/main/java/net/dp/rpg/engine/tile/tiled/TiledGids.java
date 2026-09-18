package net.dp.rpg.engine.tile.tiled;

import net.dp.rpg.engine.tile.exception.InvalidTiledFormatException;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.zip.GZIPInputStream;
import java.util.zip.Inflater;

public final class TiledGids {

  private static final long FLAG_MASK = 0x1FFFFFFFL;

  private TiledGids() {
  }

  private static int stripFlags(long rawGid) {
    return (int) (rawGid & FLAG_MASK);
  }

  public static int[] decode(String encoding, String compression, String payload, int expectedCount) {
    int[] gids = switch (encoding == null ? "xml" : encoding) {
      case "csv" -> decodeCsv(payload, expectedCount);
      case "base64" -> decodeBase64(payload, compression, expectedCount);
      default -> throw new InvalidTiledFormatException("Unsupported tile layer encoding: " + encoding);
    };

    if (gids.length != expectedCount) {
      throw new InvalidTiledFormatException("Layer holds %d cells, expected %d".formatted(gids.length, expectedCount));
    }

    return gids;
  }

  private static int[] decodeCsv(String payload, int expectedCount) {
    int[] gids = new int[expectedCount];
    int index = 0;

    for (String token : payload.split(",")) {
      String trimmed = token.trim();

      if (trimmed.isEmpty()) {
        continue;
      }

      if (index >= expectedCount) {
        throw new InvalidTiledFormatException("Layer holds more than the expected %d cells".formatted(expectedCount));
      }

      gids[index++] = stripFlags(Long.parseLong(trimmed));
    }

    if (index != expectedCount) {
      throw new InvalidTiledFormatException("Layer holds %d cells, expected %d".formatted(index, expectedCount));
    }

    return gids;
  }

  private static int[] decodeBase64(String payload, String compression, int expectedCount) {
    byte[] raw = Base64.getDecoder().decode(payload.trim());
    byte[] bytes = decompress(raw, compression);

    if (bytes.length != expectedCount * 4) {
      throw new InvalidTiledFormatException("Layer payload is %d bytes, expected %d"
          .formatted(bytes.length, expectedCount * 4));
    }

    int[] gids = new int[expectedCount];

    for (int i = 0; i < expectedCount; i++) {
      long value = (bytes[i * 4] & 0xFFL)
          | (bytes[i * 4 + 1] & 0xFFL) << 8
          | (bytes[i * 4 + 2] & 0xFFL) << 16
          | (bytes[i * 4 + 3] & 0xFFL) << 24;

      gids[i] = stripFlags(value);
    }

    return gids;
  }

  private static byte[] decompress(byte[] raw, String compression) {
    if (compression == null || compression.isBlank()) {
      return raw;
    }

    try {
      return switch (compression) {
        case "gzip" -> readAll(new GZIPInputStream(new java.io.ByteArrayInputStream(raw)));
        case "zlib" -> inflate(raw);
        default -> throw new InvalidTiledFormatException("Unsupported tile layer compression: " + compression);
      };
    } catch (IOException exception) {
      throw new InvalidTiledFormatException("Could not decompress tile layer data: " + compression, exception);
    }
  }

  private static byte[] inflate(byte[] raw) {
    Inflater inflater = new Inflater();

    try {
      inflater.setInput(raw);

      ByteArrayOutputStream output = new ByteArrayOutputStream(raw.length * 4);
      byte[] buffer = new byte[4096];

      while (!inflater.finished()) {
        int read = inflater.inflate(buffer);

        if (read == 0 && inflater.needsInput()) {
          break;
        }

        output.write(buffer, 0, read);
      }

      return output.toByteArray();
    } catch (java.util.zip.DataFormatException exception) {
      throw new InvalidTiledFormatException("Corrupt zlib tile layer data", exception);
    } finally {
      inflater.end();
    }
  }

  private static byte[] readAll(java.io.InputStream stream) throws IOException {
    try (stream) {
      return stream.readAllBytes();
    }
  }
}
