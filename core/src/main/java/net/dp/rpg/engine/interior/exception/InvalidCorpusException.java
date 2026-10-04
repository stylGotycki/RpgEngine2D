package net.dp.rpg.engine.interior.exception;

import java.util.List;
import net.dp.rpg.engine.interior.corpus.CorpusIssue;

public final class InvalidCorpusException extends InteriorException {

  private static final int LISTED_ISSUES = 20;

  private final String motif;

  private final List<CorpusIssue> issues;

  public InvalidCorpusException(String motif, List<CorpusIssue> issues) {
    super(describe(motif, issues));
    this.motif = motif;
    this.issues = List.copyOf(issues);
  }

  public String motif() {
    return motif;
  }

  public List<CorpusIssue> issues() {
    return issues;
  }

  private static String describe(String motif, List<CorpusIssue> issues) {
    List<CorpusIssue> errors = issues.stream().filter(CorpusIssue::isError).toList();
    StringBuilder message = new StringBuilder("Room corpus '%s' has %d error(s):"
        .formatted(motif, errors.size()));

    errors.stream()
        .limit(LISTED_ISSUES)
        .forEach(issue -> message.append(System.lineSeparator()).append("  ").append(issue));

    if (errors.size() > LISTED_ISSUES) {
      message.append(System.lineSeparator())
          .append("  ... and %d more".formatted(errors.size() - LISTED_ISSUES));
    }

    return message.toString();
  }
}
