package net.dp.rpg.game.quest;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.floor.FloorLayout;
import net.dp.rpg.engine.floor.graph.RoomNode;
import net.dp.rpg.engine.quest.GoalType;
import net.dp.rpg.engine.quest.Quest;
import net.dp.rpg.engine.quest.QuestGoal;
import net.dp.rpg.engine.systems.gameEvent.GameEvent;
import net.dp.rpg.engine.systems.gameEvent.GameEventListener;
import net.dp.rpg.game.TileRoom;
import net.dp.rpg.game.events.EnemyKilledEvent;
import net.dp.rpg.game.events.EnterRoomEvent;
import net.dp.rpg.game.events.GameMessageEvent;
import net.dp.rpg.game.events.ItemPickedEvent;
import net.dp.rpg.game.events.NpcActionEvent;
import net.dp.rpg.game.events.PlayerDamagedEvent;
import net.dp.rpg.game.events.PuzzleSolvedEvent;
import net.dp.rpg.game.events.QuestEvent;
import net.dp.rpg.game.scripts.ItemPickupScript;
import net.dp.rpg.game.scripts.NpcScript;

public class QuestTracker implements GameEventListener {

  public static final List<Class<? extends GameEvent>> EVENTS = List.of(EnterRoomEvent.class,
      EnemyKilledEvent.class, ItemPickedEvent.class, PlayerDamagedEvent.class,
      NpcActionEvent.class, PuzzleSolvedEvent.class);

  public enum Status {
    OFFERED,
    ACTIVE,
    COMPLETED,
    REWARDED
  }

  public interface Hooks {

    void reward(Quest quest);

    void respawnChallenge(Quest quest, QuestGoal goal);
  }

  private final class State {

    private final Quest quest;

    private final Map<String, Integer> progress = new HashMap<>();

    private final Set<String> done = new HashSet<>();

    private final List<ItemPickupScript> pickups = new ArrayList<>();

    private Status status = Status.OFFERED;

    private NpcScript giver;

    private NpcScript talker;

    private boolean damaged;

    private boolean puzzleSolved;

    private int challengeLeft;

    private State(Quest quest) {
      this.quest = quest;
      quest.goals().stream().filter(goal -> goal.type() == GoalType.CHALLENGE).findFirst()
          .ifPresent(goal -> challengeLeft = goal.count());
    }

    private boolean available(QuestGoal goal) {
      return done.containsAll(goal.requires());
    }

    private String giverName() {
      return giver == null ? "zleceniodawcy" : giver.name();
    }
  }

  private final FloorLayout layout;

  private final QuestTexts texts;

  private final Hooks hooks;

  private final Map<String, State> states = new LinkedHashMap<>();

  private final Set<Integer> visited = new HashSet<>();

  public QuestTracker(List<Quest> quests, FloorLayout layout, QuestTexts texts, Hooks hooks) {
    this.layout = layout;
    this.texts = texts;
    this.hooks = hooks;
    quests.forEach(quest -> states.put(quest.id(), new State(quest)));
  }

  public List<Quest> quests() {
    return states.values().stream().map(state -> state.quest).toList();
  }

  public Status status(String questId) {
    return states.get(questId).status;
  }

  public void registerGiver(String questId, NpcScript giver) {
    State state = states.get(questId);

    if (state != null) {
      state.giver = giver;
      giver.setVariable("quest", texts.offer(state.quest));
      giver.setVariable("progress", "");
    }
  }

  public void registerTalker(String questId, NpcScript talker) {
    State state = states.get(questId);

    if (state != null) {
      state.talker = talker;
      refreshTalker(state);
    }
  }

  public void registerPickup(ItemPickupScript pickup) {
    State state = pickup.questId() == null ? null : states.get(pickup.questId());

    if (state != null) {
      state.pickups.add(pickup);
      if (state.status == Status.OFFERED) {
        pickup.lock("Najpierw porozmawiaj z: " + state.giverName());
      }
    }
  }

  @Override
  public void onEvent(GameEvent event) {
    if (event instanceof EnterRoomEvent enter) {
      onEnter(enter.room);
    } else if (event instanceof EnemyKilledEvent killed) {
      onKill(killed);
    } else if (event instanceof ItemPickedEvent picked && picked.questId != null) {
      State state = states.get(picked.questId);

      if (state != null) {
        for (QuestGoal goal : state.quest.goals()) {
          if (goal.type() == GoalType.COLLECT && goal.target().equals(picked.item.id())) {
            state.progress.merge(goal.id(), 1, Integer::sum);
          }
        }
        evaluate(state);
      }
    } else if (event instanceof PlayerDamagedEvent damaged) {
      int room = indexOf(damaged.room);

      for (State state : states.values()) {
        if (challengeRoom(state) == room) {
          state.damaged = true;
        }
      }
    } else if (event instanceof PuzzleSolvedEvent puzzle && puzzle.questId != null) {
      State state = states.get(puzzle.questId);

      if (state != null) {
        state.puzzleSolved = true;
        evaluate(state);
      }
    } else if (event instanceof NpcActionEvent action && action.questId != null) {
      State state = states.get(action.questId);

      if (state != null) {
        onNpcAction(state, action.action);
      }
    }
  }

  private void onEnter(TileRoom room) {
    int index = indexOf(room);

    if (index < 0) {
      return;
    }

    visited.add(index);
    for (State state : states.values()) {
      QuestGoal challenge = challengeGoal(state);

      if (challenge != null && challenge.rooms().get(0) == index) {
        state.damaged = false;
        // Cleared with a scratch: the room fills up again, so the challenge never becomes
        // impossible.
        if (state.status == Status.ACTIVE && !state.done.contains(challenge.id())
            && state.challengeLeft <= 0) {
          hooks.respawnChallenge(state.quest, challenge);
          state.challengeLeft = challenge.count();
        }
      }
      evaluate(state);
    }
  }

  private void onKill(EnemyKilledEvent killed) {
    int room = indexOf(killed.room);

    for (State state : states.values()) {
      for (QuestGoal goal : state.quest.goals()) {
        if (goal.type() == GoalType.KILL && goal.rooms().contains(room)
            && goal.target().equals(killed.enemy.id())) {
          state.progress.merge(goal.id(), 1, Integer::sum);
        }
        if (goal.type() == GoalType.CHALLENGE && goal.rooms().get(0) == room) {
          state.challengeLeft--;
          state.progress.put(goal.id(), goal.count() - state.challengeLeft);
          if (state.challengeLeft <= 0 && !state.damaged && state.status == Status.ACTIVE
              && state.available(goal)) {
            markDone(state, goal);
          }
        }
      }
      evaluate(state);
    }
  }

  private void onNpcAction(State state, String action) {
    switch (action) {
      case NpcScript.ACCEPT -> {
        if (state.status == Status.OFFERED) {
          state.status = Status.ACTIVE;
          state.pickups.forEach(ItemPickupScript::unlock);
          refreshTalker(state);
          publish(state, QuestEvent.Kind.ACCEPTED, null);
          evaluate(state);
        }
      }
      case NpcScript.DECLINE -> publish(state, QuestEvent.Kind.DECLINED, null);
      case NpcScript.TALK_CORRECT -> evaluate(state);
      case NpcScript.REWARD -> {
        if (state.status == Status.COMPLETED) {
          state.status = Status.REWARDED;
          hooks.reward(state.quest);
          publish(state, QuestEvent.Kind.REWARDED, null);
        }
      }
      default -> {
      }
    }
  }

  private void evaluate(State state) {
    if (state.status != Status.ACTIVE) {
      return;
    }

    for (QuestGoal goal : state.quest.goals()) {
      if (state.done.contains(goal.id()) || !state.available(goal)) {
        continue;
      }

      boolean complete = switch (goal.type()) {
        case KILL, COLLECT -> state.progress.getOrDefault(goal.id(), 0) >= goal.count();
        case REACH -> visited.containsAll(goal.rooms());
        case TALK -> state.talker != null && state.talker.isTalkDone();
        case PUZZLE -> state.puzzleSolved;
        case CHALLENGE -> false;
      };

      if (complete) {
        markDone(state, goal);
      }
    }
  }

  private void markDone(State state, QuestGoal goal) {
    if (!state.done.add(goal.id())) {
      return;
    }

    publish(state, QuestEvent.Kind.GOAL_DONE, goal.id());
    refreshTalker(state);

    if (state.done.size() == state.quest.goals().size()) {
      state.status = Status.COMPLETED;
      if (state.giver != null) {
        state.giver.complete();
      }
      publish(state, QuestEvent.Kind.COMPLETED, null);
      message("Zadanie wykonane! Wróć do: " + state.giverName());
    } else {
      message("Cel wykonany: " + texts.goal(goal));
      evaluate(state);
    }
  }

  private void refreshTalker(State state) {
    if (state.talker == null) {
      return;
    }

    QuestGoal talk = state.quest.goals().stream().filter(goal -> goal.type() == GoalType.TALK)
        .findFirst().orElse(null);

    if (state.status == Status.OFFERED) {
      state.talker.lock("Nie znam cię. Porozmawiaj najpierw z: " + state.giverName() + ".");
    } else if (talk != null && !state.available(talk)) {
      state.talker.lock("Wróć, gdy będziesz miał coś dla mnie.");
    } else {
      state.talker.unlock();
    }
  }

  public List<String> lines() {
    List<String> lines = new ArrayList<>();

    for (State state : states.values()) {
      switch (state.status) {
        case ACTIVE -> {
          List<String> goals = new ArrayList<>();

          for (QuestGoal goal : state.quest.goals()) {
            if (!state.done.contains(goal.id()) && state.available(goal)) {
              goals.add(texts.goal(goal)
                  + texts.progress(goal, state.progress.getOrDefault(goal.id(), 0)));
            }
          }
          lines.add("[[!] " + String.join(", ", goals));
        }
        case COMPLETED -> lines.add("[#7FD36B][[X] " + texts.summary(state.quest)
            + " - wróć do: " + state.giverName() + "[]");
        case REWARDED -> lines.add("[#A8A8A8][[X] " + texts.summary(state.quest) + "[]");
        default -> {
        }
      }
    }

    return lines;
  }

  public Set<Integer> markedRooms() {
    Set<Integer> rooms = new HashSet<>();

    for (State state : states.values()) {
      switch (state.status) {
        case OFFERED, COMPLETED -> rooms.add(state.quest.giverRoom());
        case ACTIVE -> {
          for (QuestGoal goal : state.quest.goals()) {
            if (!state.done.contains(goal.id()) && state.available(goal)) {
              rooms.addAll(goal.rooms());
            }
          }
        }
        default -> {
        }
      }
    }

    return rooms;
  }

  private QuestGoal challengeGoal(State state) {
    return state.quest.goals().stream().filter(goal -> goal.type() == GoalType.CHALLENGE)
        .findFirst().orElse(null);
  }

  private int challengeRoom(State state) {
    QuestGoal goal = challengeGoal(state);

    return goal == null ? -1 : goal.rooms().get(0);
  }

  private int indexOf(TileRoom room) {
    if (room == null) {
      return -1;
    }

    RoomNode node = room.node();
    List<RoomNode> rooms = layout.graph().rooms();

    return node.index() < rooms.size() && rooms.get(node.index()) == node ? node.index() : -1;
  }

  private void publish(State state, QuestEvent.Kind kind, String goalId) {
    Engine.eventBus.publish(QuestEvent.class, e -> {
      e.kind = kind;
      e.questId = state.quest.id();
      e.template = state.quest.template();
      e.goalId = goalId;
    });
  }

  private void message(String text) {
    Engine.eventBus.publish(GameMessageEvent.class, e -> e.text = text);
  }
}
