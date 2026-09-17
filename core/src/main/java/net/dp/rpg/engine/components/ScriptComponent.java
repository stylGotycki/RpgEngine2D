package net.dp.rpg.engine.components;

import net.dp.rpg.engine.EntityScript;

public class ScriptComponent implements Component
{
    public EntityScript script;

    public ScriptComponent(EntityScript script)
    {
        this.script = script;
    }
}
