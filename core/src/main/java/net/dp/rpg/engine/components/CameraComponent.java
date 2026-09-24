package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.OrthographicCamera;

public class CameraComponent implements Component
{
    public OrthographicCamera camera;

    public CameraComponent(OrthographicCamera camera)
    {
        this.camera = camera;
    }

    @Override
    public Component copy()
    {
        OrthographicCamera newCamera = new OrthographicCamera();
        newCamera.position.set(camera.position);
        newCamera.up.set(camera.up);
        newCamera.direction.set(camera.direction);
        newCamera.view.set(camera.view);
        newCamera.zoom = camera.zoom;
        newCamera.near = camera.near;
        newCamera.far = camera.far;

        newCamera.update();

        return new CameraComponent(newCamera);
    }
}
