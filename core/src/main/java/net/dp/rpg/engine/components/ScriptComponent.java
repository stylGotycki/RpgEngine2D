package net.dp.rpg.engine.components;

import net.dp.rpg.engine.Script;

public class ScriptComponent implements Component
{
    public Script script;

    public ScriptComponent(Script script)
    {
        this.script = script;
    }
}
