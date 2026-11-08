package net.dp.rpg.engine.systems;

import com.badlogic.gdx.physics.box2d.*;
import lombok.Setter;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.FractionComponent;
import net.dp.rpg.engine.components.ScriptComponent;

public class CollisionDispatcher implements ContactListener
{
    private final Scene scene;
    private final CombatSystem combatSystem;
    @Setter
    private short attackMask = 0;
    @Setter
    private short bodyMask = 0;

    public CollisionDispatcher(Scene scene, CombatSystem combatSystem)
    {
        this.scene = scene;
        this.combatSystem = combatSystem;
    }

    @Override
    public void beginContact(Contact contact)
    {
        dispatch(contact, true);
    }

    @Override
    public void endContact(Contact contact)
    {
        dispatch(contact, false);
    }

    @Override
    public void preSolve(Contact contact, Manifold oldManifold)
    {

    }

    @Override
    public void postSolve(Contact contact, ContactImpulse impulse)
    {

    }

    private void dispatch(Contact contact, boolean begin)
    {
        Fixture fixtureA = contact.getFixtureA();
        Fixture fixtureB = contact.getFixtureB();
        int entityA = -1;
        int entityB = -1;
        if(fixtureA.getBody().getUserData() instanceof Integer)
            entityA = (int)fixtureA.getBody().getUserData();
        if(fixtureB.getBody().getUserData() instanceof Integer)
            entityB = (int)fixtureB.getBody().getUserData();

        if(begin)
        {
            FractionComponent fractionA = (FractionComponent) scene.getComponent(entityA, FractionComponent.class);
            FractionComponent fractionB = (FractionComponent) scene.getComponent(entityB, FractionComponent.class);
            if(entityA >= 0 && entityB >= 0 && (fractionA == null || fractionB == null || fractionA.fraction != fractionB.fraction))
            {
                short categoryA = fixtureA.getFilterData().categoryBits;
                short categoryB = fixtureB.getFilterData().categoryBits;
                if((categoryA & attackMask) != 0 && (categoryB & bodyMask) != 0 )
                    combatSystem.handleAttack(entityA, entityB);
                else if((categoryB & attackMask) != 0 && (categoryA & bodyMask) != 0)
                    combatSystem.handleAttack(entityB, entityA);
            }
        }
        else
        {
            combatSystem.stopAttack(entityA, entityB);
            combatSystem.stopAttack(entityB, entityA);
        }

        ScriptComponent scriptComponent = scene.getComponentStorage(ScriptComponent.class).getByEntity(entityA);
        if(scriptComponent != null)
            if(begin)
                scriptComponent.script.onCollision(entityB, fixtureA.getFilterData().categoryBits, fixtureB.getFilterData().categoryBits);
            else
                scriptComponent.script.onEndCollision(entityB, fixtureA.getFilterData().categoryBits, fixtureB.getFilterData().categoryBits);
        scriptComponent = scene.getComponentStorage(ScriptComponent.class).getByEntity(entityB);
        if(scriptComponent != null)
            if(begin)
                scriptComponent.script.onCollision(entityA, fixtureB.getFilterData().categoryBits, fixtureA.getFilterData().categoryBits);
            else
                scriptComponent.script.onEndCollision(entityA, fixtureB.getFilterData().categoryBits, fixtureA.getFilterData().categoryBits);
    }
}
