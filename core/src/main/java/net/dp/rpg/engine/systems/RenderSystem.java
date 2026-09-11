package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.maps.tiled.TiledMap;
import com.badlogic.gdx.maps.tiled.renderers.OrthogonalTiledMapRenderer;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.ScreenUtils;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ComponentStorage;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.engine.components.TextureComponent;
import net.dp.rpg.engine.components.TransformComponent;

public class RenderSystem
{
    public RenderSystem()
    {
        batch = new SpriteBatch();
    }

    public void render(Scene scene)
    {
        ScreenUtils.clear(0f, 0f, 0f, 1f);

        batch.setProjectionMatrix(scene.getCamera().combined);
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
    }

    public void dispose()
    {
        batch.dispose();
    }

    private final SpriteBatch batch;
}
