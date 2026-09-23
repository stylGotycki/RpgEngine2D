package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;

public class CameraComponent implements Component
{
    public OrthographicCamera camera;

    public CameraComponent(OrthographicCamera camera)
    {
        this.camera = camera;
    }
}
