package net.dp.rpg.game.item;

public enum WeaponClass {
  FISTS(15, 1.0, Stat.STRENGTH),
  SWORD(20, 1.0, Stat.STRENGTH),
  STAFF(20, 1.0, Stat.WISDOM),
  KNIVES(16, 1.35, Stat.AGILITY),
  // Scales with whichever of strength, agility and wisdom is highest.
  UNIQUE(22, 1.0, null);

  private final int baseDamage;

  private final double attackSpeed;

  private final Stat scaling;

  WeaponClass(int baseDamage, double attackSpeed, Stat scaling) {
    this.baseDamage = baseDamage;
    this.attackSpeed = attackSpeed;
    this.scaling = scaling;
  }

  public int baseDamage() {
    return baseDamage;
  }

  public double attackSpeed() {
    return attackSpeed;
  }

  public Stat scaling() {
    return scaling;
  }
}
