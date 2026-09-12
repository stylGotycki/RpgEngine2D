package net.dp.rpg.engine;

public abstract class AbstractGame
{
    protected AbstractGame(Engine engine)
    {
        this.engine = engine;
    }

    public abstract void create();

    public void setActiveScene(Scene scene)
    {
        engine.setActiveScene(scene);
    }

    protected final Engine engine;
}
