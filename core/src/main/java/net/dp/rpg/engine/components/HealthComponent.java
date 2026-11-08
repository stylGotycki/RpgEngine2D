package net.dp.rpg.engine.components;

public class HealthComponent implements Component
{
    public float health;
    public float maxHealth;

    public float invincibilityAfterDamage = 0;
    public float invincibilityTime = 0;

    public HealthComponent(float health, float maxHealth)
    {
        this.health = health;
        this.maxHealth = maxHealth;
    }

    public HealthComponent(float health, float maxHealth, float invincibilityAfterDamage)
    {
        this.health = health;
        this.maxHealth = maxHealth;
        this.invincibilityAfterDamage = invincibilityAfterDamage;
    }

    public void addHealth(float value)
    {
        health += value;
        if(health > maxHealth)
            health = maxHealth;
    }

    public boolean dealDamage(float value)
    {
        if(invincibilityTime <= 0)
        {
            health -= value;
            if(health < 0)
                health = 0;
            invincibilityTime = invincibilityAfterDamage;
            return true;
        }
        return false;
    }
}
