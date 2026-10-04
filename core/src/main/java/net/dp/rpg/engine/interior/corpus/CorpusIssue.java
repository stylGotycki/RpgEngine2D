package net.dp.rpg.engine.interior.corpus;

public record CorpusIssue(Severity severity, String source, int x, int y, String message) {

  public enum Severity {
    ERROR,
    WARNING
  }

  public static CorpusIssue error(String source, String message) {
    return new CorpusIssue(Severity.ERROR, source, -1, -1, message);
  }

  public static CorpusIssue errorAt(String source, int x, int y, String message) {
    return new CorpusIssue(Severity.ERROR, source, x, y, message);
  }

  public static CorpusIssue warning(String source, String message) {
    return new CorpusIssue(Severity.WARNING, source, -1, -1, message);
  }

  public static CorpusIssue warningAt(String source, int x, int y, String message) {
    return new CorpusIssue(Severity.WARNING, source, x, y, message);
  }

  public boolean isError() {
    return severity == Severity.ERROR;
  }

  @Override
  public String toString() {
    String position = x < 0 ? "" : " (%d,%d)".formatted(x, y);

    return "%s %s%s: %s".formatted(severity, source, position, message);
  }
}
