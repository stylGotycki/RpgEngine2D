package net.dp.rpg.engine.interior.model;

import java.util.Arrays;
import net.dp.rpg.engine.tile.TileType;
import net.dp.rpg.engine.tile.TileTypeRegistry;
import net.dp.rpg.engine.wfc.TokenSet;

public final class WalkableClasses {

  private final TokenSet walkable;

  private WalkableClasses(TokenSet walkable) {
    this.walkable = walkable;
  }

  public static WalkableClasses of(TileClasses classes, TileTypeRegistry types) {
    int[] walkableTokens = new int[classes.tokenCount()];
    int count = 0;

    for (int token = 0; token < classes.tokenCount(); token++) {
      if (!classes.isVoid(token) && isWalkable(classes, types, token)) {
        walkableTokens[count++] = token;
      }
    }

    TokenSet tokens = TokenSet.of(classes.tokenCount(), Arrays.copyOf(walkableTokens, count));

    return new WalkableClasses(tokens);
  }

  public boolean isWalkable(int token) {
    return walkable.contains(token);
  }

  public TokenSet walkableSubsetOf(TokenSet domain) {
    return domain.intersection(walkable);
  }

  public TokenSet all() {
    return walkable;
  }

  private static boolean isWalkable(TileClasses classes, TileTypeRegistry types, int token) {
    for (int runtimeId : classes.membersOf(token)) {
      TileType type = types.require(runtimeId);

      if (!type.walkable()) {
        return false;
      }
    }

    return true;
  }
}
