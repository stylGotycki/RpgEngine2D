package net.dp.rpg.engine.components;

import net.dp.rpg.engine.Script;

public class ScriptComponent implements Component
{
    public ScriptComponent(Script script)
    {
        this.script = script;
    }

    public Script script;
}
