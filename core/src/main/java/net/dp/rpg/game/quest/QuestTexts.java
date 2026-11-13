package net.dp.rpg.game.quest;

import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.engine.quest.Quest;
import net.dp.rpg.engine.quest.QuestGoal;
import net.dp.rpg.engine.quest.QuestPlanner;
import net.dp.rpg.game.enemy.EnemyRegistry;
import net.dp.rpg.game.item.ItemRegistry;

public class QuestTexts {

  private final EnemyRegistry enemies;

  private final ItemRegistry items;

  public QuestTexts(EnemyRegistry enemies, ItemRegistry items) {
    this.enemies = enemies;
    this.items = items;
  }

  public String goal(QuestGoal goal) {
    return switch (goal.type()) {
      case KILL -> "Zabij " + enemies.get(goal.target()).countOf(goal.count());
      case COLLECT -> goal.count() == 1 ? "Znajdź: " + items.get(goal.target()).name()
          : "Znajdź: " + items.get(goal.target()).name() + " x" + goal.count();
      case REACH -> "Dotrzyj do najdalszej komnaty";
      case CHALLENGE -> "Oczyść komnatę bez otrzymania obrażeń";
      case TALK -> QuestPlanner.RECIPIENT.equals(goal.target()) ? "Oddaj list adresatowi"
          : "Porozmawiaj z nieznajomym";
      case PUZZLE -> "Rozwiąż zagadkę komnaty";
    };
  }

  public String progress(QuestGoal goal, int value) {
    return switch (goal.type()) {
      case KILL, COLLECT, CHALLENGE -> " " + Math.min(value, goal.count()) + "/" + goal.count();
      default -> "";
    };
  }

  public String summary(Quest quest) {
    return goal(quest.goals().get(quest.goals().size() - 1));
  }

  public String offer(Quest quest) {
    QuestGoal first = quest.goals().get(0);
    String ask = switch (GoalType.valueOf(quest.template())) {
      case KILL -> "W dalszych komnatach grasują " + enemies.get(first.target()).few()
          + ". " + goal(first) + ", a dobrze ci zapłacę.";
      case COLLECT -> "Zgubiłem tu coś cennego. " + goal(first) + " i przynieś mi.";
      case REACH -> "Nikt nie wie, co jest w najdalszej komnacie. Sprawdzisz to dla mnie?";
      case CHALLENGE ->
          "Pokaż, co potrafisz: oczyść komnatę dalej i nie daj się nawet drasnąć.";
      case TALK -> quest.goals().size() > 1
          ? "Gdzieś tu leży mój list. Znajdź go i oddaj adresatowi, czeka dalej."
          : "Obok stoi ktoś dziwny. Porozmawiaj z nim, może się czegoś dowiesz.";
      case PUZZLE -> "W komnacie obok jest zagadka. Rozwiąż ją.";
    };

    return ask + " Pomożesz?";
  }
}
