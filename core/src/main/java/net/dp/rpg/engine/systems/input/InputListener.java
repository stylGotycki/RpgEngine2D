package net.dp.rpg.engine.systems.input;

public interface InputListener
{
    boolean keyDown(int keycode);
    boolean keyUp(int keycode);
}
