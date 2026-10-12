package net.dp.rpg.engine.systems;

import com.badlogic.gdx.physics.box2d.*;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ScriptComponent;

public class CollisionDispatcher implements ContactListener
{
    final Scene scene;

    public CollisionDispatcher(Scene scene)
    {
        this.scene = scene;
    }

    @Override
    public void beginContact(Contact contact)
    {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        int entityA = -1;
        int entityB = -1;
        if(fixtureA.getBody().getUserData() instanceof Integer)
            entityA = (int)fixtureA.getBody().getUserData();
        if(fixtureB.getBody().getUserData() instanceof Integer)
            entityB = (int)fixtureB.getBody().getUserData();
        ScriptComponent scriptComponent = scene.getComponentStorage(ScriptComponent.class).getByEntity(entityA);
        if(scriptComponent != null)
            scriptComponent.script.onCollision(entityB, fixtureA.getFilterData().categoryBits, fixtureB.getFilterData().categoryBits);
        scriptComponent = scene.getComponentStorage(ScriptComponent.class).getByEntity(entityB);
        if(scriptComponent != null)
            scriptComponent.script.onCollision(entityA, fixtureB.getFilterData().categoryBits, fixtureA.getFilterData().categoryBits);
    }

    @Override
    public void endContact(Contact contact)
    {

    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse)
    {

    }
}
