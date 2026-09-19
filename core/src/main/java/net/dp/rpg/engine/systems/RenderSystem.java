package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.ScreenUtils;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ComponentStorage;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.engine.components.TextureComponent;
import net.dp.rpg.engine.components.TransformComponent;

public class RenderSystem
{
    private final SpriteBatch batch;
    private final Box2DDebugRenderer b2dDebugRenderer = new Box2DDebugRenderer();

    public RenderSystem()
    {
        batch = new SpriteBatch();
    }

    public void render(Scene scene, boolean physicsDebug, boolean guiDebug)
    {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        if(scene.getActiveCamera() == null)
            return;

        Matrix4 projMatrix = scene.getActiveCamera().combined;

        batch.setProjectionMatrix(projMatrix);
        batch.begin();
        ComponentStorage<TextureComponent> textureStorage = scene.getComponentStorage(TextureComponent.class);
        ComponentStorage<TransformComponent> transformStorage = scene.getComponentStorage(TransformComponent.class);
        ComponentStorage<SpriteComponent> spriteStorage = scene.getComponentStorage(SpriteComponent.class);
        int size = textureStorage.size();

        for(int i = 0; i < size; i++)
        {
            int entity = textureStorage.getEntity(i);
            if(transformStorage.hasComponent(entity))
            {
                TransformComponent transform = transformStorage.getByEntity(entity);
                Vector2 position = transform.position;
                batch.draw(textureStorage.getByIndex(i).texture, position.x, position.y);
            }
        }

        size = spriteStorage.size();

        for(int i = 0; i < size; i++)
        {
            spriteStorage.getByIndex(i).sprite.draw(batch);
        }

        batch.end();

        scene.getGui().getStage().draw();

        if(physicsDebug)
            b2dDebugRenderer.render(scene.getPhysicalWorld(), projMatrix);
    }

    public void dispose()
    {
        batch.dispose();
        b2dDebugRenderer.dispose();
    }
}
