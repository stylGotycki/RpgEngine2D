package net.dp.rpg.engine.adaptation;

import java.util.List;
import net.dp.rpg.engine.content.ContentSettings;
import net.dp.rpg.engine.floor.FloorSettings;
import net.dp.rpg.engine.quest.QuestWeights;

public record Adaptation(PlayerProfile profile, FloorSettings floor, ContentSettings content,
    QuestWeights quests, List<String> reasons) {

  public Adaptation {
    reasons = List.copyOf(reasons);
  }
}
