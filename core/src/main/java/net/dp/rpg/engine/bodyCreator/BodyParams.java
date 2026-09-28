package net.dp.rpg.engine.bodyCreator;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.BodyDef;
import lombok.Builder;

@Builder
public class BodyParams
{
    public BodyDef.BodyType type;
    public Vector2 position;
    public Boolean fixedRotation;
    public Float linearDamping;
    public Float angularDamping;
}
