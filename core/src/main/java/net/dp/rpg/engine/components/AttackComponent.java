package net.dp.rpg.engine.components;

import com.badlogic.gdx.utils.IntSet;

public class AttackComponent implements Component
{
    public float damage;
    public float knockback;
    public final IntSet hitEntities = new IntSet();

    public AttackComponent(float damage, float knockback)
    {
        this.damage = damage;
        this.knockback = knockback;
    }

    public void clearHitEntities()
    {
        hitEntities.clear();
    }
}
