package net.dp.rpg.engine.components;

public class PeriodicAttackComponent implements Component
{
    public float damage;
    public float knockback;
    public float damageDelay;
    public float attackTimer = 0;

    public PeriodicAttackComponent(float damage, float knockback, float damageDelay)
    {
        this.damage = damage;
        this.knockback = knockback;
        this.damageDelay = damageDelay;
    }

    public boolean canAttack()
    {
        return attackTimer <= 0;
    }

    public void updateTimer(float delta)
    {
        attackTimer -= delta;
        if(attackTimer < 0)
            attackTimer = 0;
    }

    public void resetTimer()
    {
        attackTimer = damageDelay;
    }

}
