package net.dp.rpg.engine.components;

import com.badlogic.gdx.graphics.OrthographicCamera;
import com.badlogic.gdx.math.Rectangle;

public class CameraComponent implements Component
{
    public OrthographicCamera camera;
    public Rectangle boundingRectangle;
    public boolean isBounded = false;

    public CameraComponent(OrthographicCamera camera)
    {
        this.camera = camera;
    }

    public CameraComponent(OrthographicCamera camera, Rectangle boundingRectangle)
    {
        this.camera = camera;
        this.isBounded = true;
        this.boundingRectangle = boundingRectangle;
    }
}
