package net.dp.rpg.engine.components;

import com.badlogic.gdx.physics.box2d.Body;
import com.badlogic.gdx.physics.box2d.BodyDef;
import com.badlogic.gdx.physics.box2d.Fixture;
import com.badlogic.gdx.physics.box2d.FixtureDef;
import com.badlogic.gdx.utils.Array;

public class PhysicalBodyComponent implements Component
{
    public Body body;

    public PhysicalBodyComponent(Body body)
    {
        this.body = body;
    }

    @Override
    public Component copy()
    {
        BodyDef bodyDef = new BodyDef();
        bodyDef.type = body.getType();
        bodyDef.angularDamping = body.getAngularDamping();
        bodyDef.linearDamping = body.getAngularDamping();
        bodyDef.fixedRotation = body.isFixedRotation();
        bodyDef.position.x = body.getPosition().x;
        bodyDef.position.y = body.getPosition().y;
        bodyDef.active = body.isActive();
        bodyDef.allowSleep = body.isSleepingAllowed();
        bodyDef.angle = body.getAngle();
        bodyDef.angularVelocity = body.getAngularVelocity();
        bodyDef.awake = body.isAwake();
        bodyDef.bullet = body.isBullet();
        bodyDef.gravityScale = body.getGravityScale();
        bodyDef.linearVelocity.x = body.getLinearVelocity().x;
        bodyDef.linearVelocity.y = body.getLinearVelocity().y;

        Body newBody = body.getWorld().createBody(bodyDef);

        Array<Fixture> fixtures = body.getFixtureList();
        FixtureDef newFixtureDef = new FixtureDef();
        for (Fixture fixture : fixtures)
        {
            newFixtureDef.shape = fixture.getShape();
            newFixtureDef.isSensor = fixture.isSensor();
            newFixtureDef.friction = fixture.getFriction();
            newFixtureDef.density = fixture.getDensity();
            newFixtureDef.restitution = fixture.getRestitution();
            newFixtureDef.filter.set(fixture.getFilterData());
            newBody.createFixture(newFixtureDef);
        }
        return new PhysicalBodyComponent(newBody);
    }
}
