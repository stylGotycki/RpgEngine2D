package net.dp.rpg.engine;

import com.badlogic.gdx.math.Vector2;
import net.dp.rpg.engine.tile.room.Direction;

public class Utils
{
    public static Vector2 directionToVector2(Direction direction, Vector2 vector2)
    {
        return vector2.set(direction.getDeltaX(), direction.getDeltaY());
    }

    public static float getFrameChance(float perSecond, float deltaTime)
    {
        if(perSecond <= 0)
            return 0;
        return (float)(1 - Math.exp(-perSecond * deltaTime));
    }
}
