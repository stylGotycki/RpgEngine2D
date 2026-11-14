package net.dp.rpg.engine.dialogue;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public record Dialogue(String id, String start, Map<String, DialogueNode> nodes) {

  public Dialogue {
    nodes = new LinkedHashMap<>(nodes);
  }

  public DialogueNode node(String nodeId) {
    DialogueNode node = nodes.get(nodeId);

    if (node == null) {
      throw new IllegalArgumentException("Dialogue " + id + " has no node " + nodeId);
    }

    return node;
  }

  public List<String> problems() {
    List<String> problems = new ArrayList<>();

    if (!nodes.containsKey(start)) {
      problems.add(id + ": start node '" + start + "' does not exist");
      return problems;
    }

    for (DialogueNode node : nodes.values()) {
      String where = id + "/" + node.id();

      if (node.text() == null || node.text().isBlank()) {
        problems.add(where + ": no text");
      }
      if (node.hasChoices() && node.next() != null) {
        problems.add(where + ": has both choices and next");
      }
      if (node.choices().size() > DialogueNode.MAX_CHOICES) {
        problems.add(where + ": more than " + DialogueNode.MAX_CHOICES + " choices");
      }
      if (node.next() != null && !nodes.containsKey(node.next())) {
        problems.add(where + ": next '" + node.next() + "' does not exist");
      }
      for (DialogueChoice choice : node.choices()) {
        if (choice.next() != null && !nodes.containsKey(choice.next())) {
          problems.add(where + ": choice '" + choice.text() + "' leads to missing '"
              + choice.next() + "'");
        }
      }
    }

    Set<String> reached = reachable();
    for (String nodeId : nodes.keySet()) {
      if (!reached.contains(nodeId)) {
        problems.add(id + "/" + nodeId + ": unreachable from '" + start + "'");
      }
    }

    return problems;
  }

  private Set<String> reachable() {
    Set<String> reached = new HashSet<>();
    Deque<String> pending = new ArrayDeque<>(List.of(start));

    while (!pending.isEmpty()) {
      String nodeId = pending.pop();
      DialogueNode node = nodes.get(nodeId);

      if (node == null || !reached.add(nodeId)) {
        continue;
      }
      if (node.next() != null) {
        pending.push(node.next());
      }
      for (DialogueChoice choice : node.choices()) {
        if (choice.next() != null) {
          pending.push(choice.next());
        }
      }
    }

    return reached;
  }
}
