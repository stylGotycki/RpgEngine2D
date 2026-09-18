package net.dp.rpg.engine.tile.tiled;

import java.util.ArrayDeque;
import java.util.Deque;

final class XmlWriter {

  private static final String INDENT = " ";

  private static final String NEW_LINE = System.lineSeparator();

  private final StringBuilder out = new StringBuilder(8192);

  private final Deque<String> openElements = new ArrayDeque<>();

  private boolean startTagPending;

  XmlWriter declaration() {
    out.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>").append(NEW_LINE);

    return this;
  }

  XmlWriter open(String name) {
    finishStartTag();
    indent();

    out.append('<').append(name);
    openElements.push(name);
    startTagPending = true;

    return this;
  }

  XmlWriter attr(String name, String value) {
    requirePendingTag(name);

    out.append(' ').append(name).append("=\"").append(escape(value)).append('"');

    return this;
  }

  XmlWriter attr(String name, long value) {
    requirePendingTag(name);

    out.append(' ').append(name).append("=\"").append(value).append('"');

    return this;
  }

  XmlWriter attr(String name, double value) {
    return attr(name, number(value));
  }

  XmlWriter attrIfPresent(String name, String value) {
    return value == null || value.isBlank() ? this : attr(name, value);
  }

  XmlWriter textBlock(String text) {
    finishStartTag();

    out.append(text);

    if (!text.endsWith(NEW_LINE)) {
      out.append(NEW_LINE);
    }

    return this;
  }

  XmlWriter blankLine() {
    finishStartTag();

    out.append(NEW_LINE);

    return this;
  }

  XmlWriter close() {
    if (openElements.isEmpty()) {
      throw new IllegalStateException("No open element to close");
    }

    String name = openElements.pop();

    if (startTagPending) {
      out.append("/>").append(NEW_LINE);
      startTagPending = false;
    }
    else {
      indent();
      out.append("</").append(name).append('>').append(NEW_LINE);
    }

    return this;
  }

  @Override
  public String toString() {
    if (!openElements.isEmpty()) {
      throw new IllegalStateException("Unclosed elements: " + openElements);
    }

    return out.toString();
  }

  private void finishStartTag() {
    if (startTagPending) {
      out.append('>').append(NEW_LINE);
      startTagPending = false;
    }
  }

  private void requirePendingTag(String attribute) {
    if (!startTagPending) {
      throw new IllegalStateException(
          "Attribute '%s' written after the element body started".formatted(attribute));
    }
  }

  private void indent() {
    out.append(INDENT.repeat(openElements.size()));
  }

  private static String number(double value) {
    if (value == Math.rint(value) && !Double.isInfinite(value)) {
      return Long.toString((long) value);
    }

    return Double.toString(value);
  }

  private static String escape(String value) {
    return value.replace("&", "&amp;")
        .replace("<", "&lt;")
        .replace(">", "&gt;")
        .replace("\"", "&quot;");
  }
}
