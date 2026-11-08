package net.dp.rpg.engine.interior.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import net.dp.rpg.engine.tile.TileGrid;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;

public final class TileClasses {

  public static final int VOID = 0;

  private static final String VOID_NAME = "VOID";

  private final TileTypeRegistry types;

  private final Map<Integer, Integer> classOfRuntimeId = new LinkedHashMap<>();

  private final List<String> classNames = new ArrayList<>(List.of(VOID_NAME));

  private final List<List<Integer>> membersByClass = new ArrayList<>(List.of(List.of()));

  private TileClasses(TileTypeRegistry types) {
    this.types = types;
  }

  public static TileClasses of(TileTypeRegistry types, Map<String, List<String>> groups) {
    TileClasses classes = new TileClasses(types);
    Map<String, String> groupOfTypeId = new LinkedHashMap<>();

    groups.forEach((groupName, members) -> {
      if (members.isEmpty()) {
        throw new IllegalArgumentException("Class group '%s' names no tile types".formatted(
            groupName));
      }

      for (String typeId : members) {
        String existing = groupOfTypeId.putIfAbsent(typeId, groupName);

        if (existing != null) {
          throw new IllegalArgumentException("Tile type '%s' is in both class groups '%s' and '%s'"
              .formatted(typeId, existing, groupName));
        }
      }

      classes.addClass(groupName, members.stream().map(types::requireRuntimeId).toList());
    });

    for (TileType type : types.all()) {
      if (!groupOfTypeId.containsKey(type.id())) {
        classes.addClass(type.id(), List.of(types.requireRuntimeId(type.id())));
      }
    }

    return classes;
  }

  public int tokenCount() {
    return classNames.size();
  }

  public boolean isVoid(int token) {
    return token == VOID;
  }

  public String nameOf(int token) {
    return classNames.get(token);
  }

  public List<Integer> membersOf(int token) {
    return membersByClass.get(token);
  }

  public TileType representativeOf(int token) {
    if (isVoid(token)) {
      throw new IllegalArgumentException("VOID has no tile type");
    }

    return types.require(membersOf(token).get(0));
  }

  public int skinOf(int token, Random random) {
    List<Integer> members = membersOf(token);

    if (members.size() == 1) {
      return members.getFirst();
    }

    double total = 0.0;

    for (int runtimeId : members) {
      total += types.require(runtimeId).defaultWeight();
    }

    double target = random.nextDouble() * total;

    for (int runtimeId : members) {
      target -= types.require(runtimeId).defaultWeight();

      if (target < 0.0) {
        return runtimeId;
      }
    }

    return members.get(members.size() - 1);
  }

  public int classOf(int runtimeId) {
    if (runtimeId == TileGrid.EMPTY) {
      return VOID;
    }

    Integer token = classOfRuntimeId.get(runtimeId);

    if (token == null) {
      throw new IllegalArgumentException("Runtime tile id %d belongs to no known class"
          .formatted(runtimeId));
    }

    return token;
  }

  public int tokenOf(String className) {
    int index = classNames.indexOf(className);

    if (index < 0) {
      throw new IllegalArgumentException("Unknown tile class: " + className);
    }

    return index;
  }

  private void addClass(String name, List<Integer> runtimeIds) {
    int token = classNames.size();

    classNames.add(name);
    membersByClass.add(List.copyOf(runtimeIds));

    for (int runtimeId : runtimeIds) {
      Integer existing = classOfRuntimeId.putIfAbsent(runtimeId, token);

      if (existing != null) {
        throw new IllegalArgumentException(
            "Runtime tile id %d is in more than one class".formatted(runtimeId));
      }
    }
  }
}
