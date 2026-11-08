package net.dp.rpg.engine.systems;

import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Body;
import net.dp.rpg.engine.Script;
import net.dp.rpg.engine.components.*;

import java.util.HashSet;
import java.util.Set;

public class CombatSystem
{
    private final ComponentStorage<HealthComponent> healthComponents;
    private final ComponentStorage<AttackComponent> attackComponents;
    private final ComponentStorage<PeriodicAttackComponent> periodicAttackComponents;
    private final ComponentStorage<PhysicalBodyComponent> bodyComponents;
    private final ComponentStorage<ScriptComponent> scriptComponents;
    private Vector2 push = new Vector2();

    private record PeriodicDamageKey(int attacker, int attacked){}

    private final Set<PeriodicDamageKey> periodicDamageContacts = new HashSet<>();

    public CombatSystem(ComponentStorage<HealthComponent> healthComponents,
                        ComponentStorage<AttackComponent> attackComponents,
                        ComponentStorage<PhysicalBodyComponent> bodyComponents,
                        ComponentStorage<PeriodicAttackComponent> periodicAttackComponents,
                        ComponentStorage<ScriptComponent> scriptComponents)
    {
        this.healthComponents = healthComponents;
        this.attackComponents = attackComponents;
        this.periodicAttackComponents = periodicAttackComponents;
        this.bodyComponents = bodyComponents;
        this.scriptComponents = scriptComponents;
    }

    public void handleAttack(int attacker, int attacked)
    {
        AttackComponent attack = attackComponents.getByEntity(attacker);
        PeriodicAttackComponent periodicAttack = periodicAttackComponents.getByEntity(attacker);
        if(attack != null)
        {
            if(attack.hitEntities.contains(attacked))
                return;
            else
                attack.hitEntities.add(attacked);
            dealDamageIfPossible(attacker, attacked, attack.damage, attack.knockback);
        }
        else if(periodicAttack != null)
        {
            periodicDamageContacts.add(new PeriodicDamageKey(attacker, attacked));
        }
    }

    public void stopAttack(int attacker, int attacked)
    {
        periodicDamageContacts.remove(new PeriodicDamageKey(attacker, attacked));
    }

    public void updatePeriodicAttack(float delta)
    {
        int size = periodicAttackComponents.size();
        for(int i = 0; i < size; i++)
        {
            periodicAttackComponents.getByIndex(i).updateTimer(delta);
        }

        for(PeriodicDamageKey contact : periodicDamageContacts)
        {
            PeriodicAttackComponent periodicAttack = periodicAttackComponents.getByEntity(contact.attacker);
            if(!periodicAttack.canAttack())
                continue;
            if(dealDamageIfPossible(contact.attacker, contact.attacked, periodicAttack.damage, periodicAttack.knockback))
            {
                periodicAttack.resetTimer();
            }
        }
    }

    private boolean dealDamageIfPossible(int attacker, int attacked, float damage, float knockback)
    {
        HealthComponent health = healthComponents.getByEntity(attacked);
        if(health != null && health.dealDamage(damage))
        {
            Body attackedBody = bodyComponents.getByEntity(attacked).body;
            Body attackerBody = bodyComponents.getByEntity(attacker).body;
            pushBody(attackedBody, attackerBody.getPosition(), knockback);
            ScriptComponent scriptComponent = scriptComponents.getByEntity(attacked);
            if(scriptComponent != null)
                scriptComponent.script.onDamage();
            return true;
        }
        return false;
    }

    private void pushBody(Body body, Vector2 from, float force)
    {
        push = push.set(body.getPosition()).sub(from).scl(force);
        body.applyLinearImpulse(push, body.getPosition(), true);
    }
}
