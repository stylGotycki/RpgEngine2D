package net.dp.rpg.engine;

import com.badlogic.gdx.math.Vector2;
import net.dp.rpg.engine.tile.room.Direction;

public class Utils
{
    public static Vector2 directionToVector2(Direction direction, Vector2 vector2)
    {
        return vector2.set(direction.getDeltaX(), direction.getDeltaY());
    }
}
