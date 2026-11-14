package net.dp.rpg.engine.floor;

// How branches grow. chaining: chance that the next branch room hangs on the previous one, so
// branches become long corridors instead of single rooms. deadEndMinibosses: chance that a dead
// end off the critical path holds a miniboss.
public record BranchSettings(double chaining, double deadEndMinibosses) {

  public static final BranchSettings NONE = new BranchSettings(0, 0);

  public BranchSettings {
    if (chaining < 0 || chaining > 1 || deadEndMinibosses < 0 || deadEndMinibosses > 1) {
      throw new IllegalArgumentException("Branch chances must be within 0..1, got " + chaining
          + ", " + deadEndMinibosses);
    }
  }
}
