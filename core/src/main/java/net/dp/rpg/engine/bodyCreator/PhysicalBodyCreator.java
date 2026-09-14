package net.dp.rpg.engine.bodyCreator;

import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import lombok.Getter;

public class PhysicalBodyCreator
{
    @Getter
    private FixtureDef fixtureDef = new FixtureDef();
    @Getter
    private BodyDef bodyDef = new BodyDef();

    public void setBodyDefParams(BodyParams params)
    {
        if(params.type != null) bodyDef.type = params.type;
        if(params.position != null) bodyDef.position.set(params.position);
        if(params.fixedRotation != null) bodyDef.fixedRotation = params.fixedRotation;
        if(params.linearDamping != null) bodyDef.linearDamping = params.linearDamping;
        if(params.angularDamping != null) bodyDef.angularDamping = params.angularDamping;
    }

    public void setFixtureDefParams(FixtureParams params)
    {
        if(params.shape != null) fixtureDef.shape = params.shape;
        if(params.restitution != null) fixtureDef.restitution = params.restitution;
        if(params.density != null) fixtureDef.density = params.density;
        if(params.friction != null) fixtureDef.friction = params.friction;
        if(params.isSensor != null) fixtureDef.isSensor = params.isSensor;
    }

    public void resetBodyDefParams()
    {
        bodyDef = new BodyDef();
    }

    public void resetFixtureDefParams()
    {
        fixtureDef = new FixtureDef();
    }
}
