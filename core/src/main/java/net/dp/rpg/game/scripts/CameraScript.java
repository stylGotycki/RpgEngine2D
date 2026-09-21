package net.dp.rpg.game.scripts;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Vector2;
import net.dp.rpg.engine.EntityScript;
import net.dp.rpg.engine.components.CameraComponent;
import net.dp.rpg.engine.components.PhysicalBodyComponent;

public class CameraScript extends EntityScript
{
    private int etiEntity;

    public CameraScript(int etiEntity)
    {
        this.etiEntity = etiEntity;
    }

    public void update(float delta)
    {
        OrthographicCamera camera = getScene().getComponentStorage(CameraComponent.class).getByEntity(getEntity()).camera;
        Vector2 etiPosition = getScene().getComponentStorage(PhysicalBodyComponent.class).getByEntity(etiEntity).body.getPosition();

        camera.position.x = etiPosition.x;
        camera.position.y = etiPosition.y;
    }
}
