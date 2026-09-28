package net.dp.rpg.engine.floor.shape;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

/**
 * Draws shapes for one floor.
 *
 * <p>The group lock lives here rather than in a pool, because a signature shape is meant to appear once per floor
 * and there are many pools. Once a group is locked its mass renormalises proportionally over what is left.
 *
 * <p>A draw does not lock anything; only {@link #confirm} does. A caller may discard a draw that does not fit,
 * and locking on the draw would let a discarded signature shape silence signatures for the rest of the floor.
 *
 * <p>Large shapes are throttled by {@code budgetFactor^(size-1)}: a single cell keeps full weight at exponent
 * zero, while a ring at exponent seven loses a factor of 128 once the factor halves. Cost penalises itself.
 *
 * <p>The factor stays at one while most of the slack is untouched and only falls over the last
 * {@link #THROTTLE_START} of it. An earlier-biting curve suppressed large shapes throughout a walk even though
 * the budget was never close to binding: measured over 3000 floors it cost about 15 points of signature rate
 * while average use sat at 21 cells of a 29-cell budget. Overrun is already prevented outright by the hard
 * reserve in {@link #draw}, so the throttle only has to make the soft limit graceful near the end.
 */
public final class ShapeDrawContext {

  public static final double THROTTLE_START = 0.5;

  private final Set<String> lockedGroups = new LinkedHashSet<>();

  private boolean exclusiveGroups;

  public ShapeDrawContext(boolean exclusiveGroups) {
    this.exclusiveGroups = exclusiveGroups;
  }

  public void setExclusiveGroups(boolean enabled) {
    this.exclusiveGroups = enabled;

    if (!enabled) {
      lockedGroups.clear();
    }
  }

  public Set<String> lockedGroups() {
    return Set.copyOf(lockedGroups);
  }

  public ShapeVariant draw(ShapePool pool, int roomsLeft, int cellsLeft, double budgetFactor, Random random) {
    List<ShapePool.ShapeEntry> candidates = new ArrayList<>();
    List<Double> weights = new ArrayList<>();
    double total = 0.0;

    for (ShapePool.ShapeEntry entry : pool.entries()) {
      int size = entry.shape().size();

      if (size > cellsLeft - (roomsLeft - 1) || isLocked(entry.shape())) {
        continue;
      }

      double weight = entry.weight() * Math.pow(budgetFactor, size - 1);

      candidates.add(entry);
      weights.add(weight);
      total += weight;
    }

    if (candidates.isEmpty() || total <= 0.0) {
      return variantOf(pool.smallest(), random);
    }

    return variantOf(pick(candidates, weights, total, random), random);
  }

  public void confirm(ShapeVariant variant) {
    RoomShapeDef shape = variant.definition();

    if (exclusiveGroups && shape.grouped()) {
      lockedGroups.add(shape.group());
    }
  }

  public static double budgetFactor(int cellBudget, int roomBudget, int cellsUsed, int roomsPlaced) {
    int cellsLeft = cellBudget - cellsUsed;
    int roomsLeft = Math.max(1, roomBudget - roomsPlaced);
    double initialSlack = Math.max(1, cellBudget - roomBudget);
    double remainingSlack = cellsLeft - roomsLeft;

    return Math.clamp(remainingSlack / (initialSlack * THROTTLE_START), 0.0, 1.0);
  }

  private ShapeVariant variantOf(ShapePool.ShapeEntry entry, Random random) {
    List<ShapeVariant> variants = entry.shape().variants();

    return variants.get(random.nextInt(variants.size()));
  }

  private boolean isLocked(RoomShapeDef shape) {
    return exclusiveGroups && shape.grouped() && lockedGroups.contains(shape.group());
  }

  private ShapePool.ShapeEntry pick(List<ShapePool.ShapeEntry> candidates, List<Double> weights, double total,
                                    Random random) {
    double roll = random.nextDouble() * total;

    for (int index = 0; index < candidates.size(); index++) {
      roll -= weights.get(index);

      if (roll <= 0.0) {
        return candidates.get(index);
      }
    }

    return candidates.get(candidates.size() - 1);
  }
}
