package net.dp.rpg.engine.systems;

import com.badlogic.gdx.graphics.g2d.Sprite;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Matrix4;
import com.badlogic.gdx.physics.box2d.Box2DDebugRenderer;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.ScreenUtils;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.engine.components.ComponentStorage;
import net.dp.rpg.engine.components.SpriteComponent;
import net.dp.rpg.engine.tile.TileSystem;
import net.dp.rpg.engine.tile.render.TileMapRenderer;

public class RenderSystem
{
    private final SpriteBatch batch;
    private final Box2DDebugRenderer b2dDebugRenderer = new Box2DDebugRenderer();
    private TileMapRenderer tileMapRenderer;
    private final Matrix4 tileTransformMatrix = new Matrix4();
    private final Matrix4 transformMatrix = new Matrix4();

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

        if(tileMapRenderer == null || scene.getTileSystem() == null)
        {
            TileSystem tileSystem = scene.getTileSystem();
            if(tileSystem != null)
            {
                tileMapRenderer = scene.getTileSystem().createRenderer();
                tileMapRenderer.setBatch(batch);
            }
        }
        else
        {
            scene.getActiveCamera().position.sub(scene.getTileTransformX(), scene.getTileTransformY(), 0);
            tileMapRenderer.setBatch(batch);
            tileTransformMatrix.set(batch.getTransformMatrix());
            tileTransformMatrix.translate(scene.getTileTransformX(), scene.getTileTransformY(), 0);
            batch.setTransformMatrix(tileTransformMatrix);
            tileMapRenderer.render(scene.getTileMap(), scene.getActiveCamera());
            scene.getActiveCamera().position.add(scene.getTileTransformX(), scene.getTileTransformY(), 0);
        }
        batch.setTransformMatrix(transformMatrix);
        batch.setProjectionMatrix(projMatrix);
        batch.begin();
        ComponentStorage<SpriteComponent> spriteStorage = scene.getComponentStorage(SpriteComponent.class);

        int size = spriteStorage.size();

        for(int i = 0; i < size; i++)
        {
            Array<Sprite> sprites = spriteStorage.getByIndex(i).sprites;
            for(Sprite sprite : sprites)
                sprite.draw(batch);
        }

        batch.end();

        // lights
        scene.getRayHandler().setCombinedMatrix(scene.getActiveCamera());
        scene.getRayHandler().updateAndRender();

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
