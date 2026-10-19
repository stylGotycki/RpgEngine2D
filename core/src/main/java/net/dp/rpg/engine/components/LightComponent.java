package net.dp.rpg.engine.components;

import box2dLight.Light;

public class LightComponent implements Component
{
    public Light light;

    public LightComponent(Light light)
    {
        this.light = light;
    }

    @Override
    public Component copy()
    {
        //todo
        return null;
    }
}
