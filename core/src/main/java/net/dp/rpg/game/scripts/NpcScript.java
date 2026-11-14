package net.dp.rpg.game.scripts;

import com.badlogic.gdx.graphics.g2d.Sprite;
import java.util.HashMap;
import java.util.Map;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.dialogue.Dialogue;
import net.dp.rpg.engine.dialogue.DialogueLibrary;
import net.dp.rpg.game.dialogue.DialogueController;
import net.dp.rpg.game.level.events.NpcActionEvent;

public class NpcScript extends EntityScript {

  public static final String ACCEPT = "quest-accept";

  public static final String DECLINE = "quest-decline";

  public static final String REWARD = "quest-reward";

  public static final String TALK_CORRECT = "talk-correct";

  public static final String TALK_WRONG = "talk-wrong";

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

  private String locked;

  private boolean talkDone;

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

  public void lock(String line) {
    locked = line;
    refreshMarker();
  }

  public void unlock() {
    locked = null;
    refreshMarker();
  }

  public boolean isLocked() {
    return locked != null;
  }

  public boolean isTalkDone() {
    return talkDone;
  }

  // The tracker only completes quests that were taken, so OFFERING here just means the accept
  // reached the tracker before this NPC (it does not in the game, it can in tests).
  public void complete() {
    if (state == State.ACCEPTED || state == State.OFFERING) {
      state = State.COMPLETED;
      refreshMarker();
    }
  }

  // Declining changes nothing, so the next talk tells the story and asks again.
  public void talk(DialogueController dialogues, DialogueLibrary library) {
    String id = switch (role) {
      case TALK -> locked != null ? "talk.locked" : talkDone ? "talk.done" : dialogueId;
      case QUEST_GIVER -> switch (state) {
        case OFFERING -> dialogueId;
        case ACCEPTED -> "giver.waiting";
        case COMPLETED -> "giver.done";
        case REWARDED -> "giver.finished";
      };
    };
    Dialogue dialogue = library.get(id);

    variables.put("locked", locked == null ? "" : locked);
    dialogues.open(dialogue, variables, this::onAction);
  }

  // The NPC changes first, then tells everyone: a listener may react by completing the quest.
  private void onAction(String action) {
    if (TALK_CORRECT.equals(action)) {
      talkDone = true;
    } else if (ACCEPT.equals(action) && state == State.OFFERING) {
      state = State.ACCEPTED;
    } else if (REWARD.equals(action) && state == State.COMPLETED) {
      state = State.REWARDED;
    }
    refreshMarker();

    Engine.eventBus.publish(NpcActionEvent.class, e -> {
      e.questId = questId;
      e.action = action;
      e.npcName = name();
    });
  }

  // "!" while the player has something to do here: take the quest or collect the reward.
  private void refreshMarker() {
    if (marker != null) {
      boolean shown = role == Role.QUEST_GIVER
          ? state == State.OFFERING || state == State.COMPLETED
          : questId != null && locked == null && !talkDone;
      marker.setAlpha(shown ? 1f : 0f);
    }
  }

  @Override
  public void update(float delta) {
  }
}
