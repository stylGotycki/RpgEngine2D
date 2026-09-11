package net.dp.rpg.engine.bodyCreator;

import com.badlogic.gdx.physics.box2d.Shape;
import lombok.Builder;

@Builder
public class FixtureParams
{
    public Shape shape;
    public Float restitution;
    public Float density;
    public Float friction;
    public Boolean isSensor;
}
