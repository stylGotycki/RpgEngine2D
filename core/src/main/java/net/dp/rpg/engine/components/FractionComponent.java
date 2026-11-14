package net.dp.rpg.engine.components;

public class FractionComponent implements Component
{
    public AbstractFraction fraction;

    public FractionComponent(AbstractFraction fraction)
    {
        this.fraction = fraction;
    }
}
