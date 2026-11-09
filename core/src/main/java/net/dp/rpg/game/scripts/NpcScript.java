package net.dp.rpg.game.scripts;

import com.badlogic.gdx.graphics.g2d.Sprite;
import java.util.HashMap;
import java.util.Map;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.dialogue.Dialogue;
import net.dp.rpg.engine.dialogue.DialogueLibrary;
import net.dp.rpg.game.dialogue.DialogueController;

public class NpcScript extends EntityScript {

  public static final String ACCEPT = "quest-accept";

  public static final String DECLINE = "quest-decline";

  public static final String REWARD = "quest-reward";

  public enum Role {
    QUEST_GIVER,
    TALK
  }

  public enum State {
    OFFERING,
    ACCEPTED,
    COMPLETED,
    REWARDED
  }

  private final Role role;

  private final String dialogueId;

  private final String questId;

  private final Map<String, String> variables = new HashMap<>();

  private State state = State.OFFERING;

  private Sprite marker;

  public NpcScript(Role role, String name, String dialogueId, String questId) {
    this.role = role;
    this.dialogueId = dialogueId;
    this.questId = questId;
    variables.put("name", name);
  }

  public Role role() {
    return role;
  }

  public String questId() {
    return questId;
  }

  public State state() {
    return state;
  }

  public String name() {
    return variables.get("name");
  }

  public void setVariable(String key, String value) {
    variables.put(key, value);
  }

  public void setMarker(Sprite marker) {
    this.marker = marker;
    refreshMarker();
  }

  public void complete() {
    if (state == State.ACCEPTED) {
      state = State.COMPLETED;
      refreshMarker();
    }
  }

  // Declining changes nothing, so the next talk tells the story and asks again.
  public void talk(DialogueController dialogues, DialogueLibrary library) {
    String id = switch (role) {
      case TALK -> dialogueId;
      case QUEST_GIVER -> switch (state) {
        case OFFERING -> dialogueId;
        case ACCEPTED -> "giver.waiting";
        case COMPLETED -> "giver.done";
        case REWARDED -> "giver.finished";
      };
    };
    Dialogue dialogue = library.get(id);

    dialogues.open(dialogue, variables, this::onAction);
  }

  private void onAction(String action) {
    if (ACCEPT.equals(action) && state == State.OFFERING) {
      state = State.ACCEPTED;
    } else if (REWARD.equals(action) && state == State.COMPLETED) {
      state = State.REWARDED;
    }
    refreshMarker();
  }

  // "!" while the player has something to do here: take the quest or collect the reward.
  private void refreshMarker() {
    if (marker != null) {
      boolean shown = role == Role.QUEST_GIVER
          && (state == State.OFFERING || state == State.COMPLETED);
      marker.setAlpha(shown ? 1f : 0f);
    }
  }

  @Override
  public void update(float delta) {
  }
}
