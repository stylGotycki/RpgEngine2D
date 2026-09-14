package net.dp.rpg.engine;

public abstract class AbstractGame
{
    protected final Engine engine;

    protected AbstractGame(Engine engine)
    {
        this.engine = engine;
    }

    public abstract void create();

    public void setActiveScene(Scene scene)
    {
        engine.setActiveScene(scene);
    }
}
