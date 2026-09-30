package net.dp.rpg.engine.components;

public class HealthComponent implements Component
{
    public float health;
    public float maxHealth;

    public HealthComponent(float health, float maxHealth)
    {
        this.health = health;
        this.maxHealth = maxHealth;
    }

    @Override
    public Component copy()
    {
        return new HealthComponent(health, maxHealth);
    }

    public void addHealth(float value)
    {
        health += value;
        if(health > maxHealth)
            health = maxHealth;
    }

    public void dealDamage(float value)
    {
        health -= value;
        if(health < 0)
            health = 0;
    }
}
