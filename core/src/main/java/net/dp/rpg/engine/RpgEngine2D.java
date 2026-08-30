package net.dp.rpg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.Input;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.math.Vector2;
import net.dp.rpg.engine.components.MoveComponent;
import net.dp.rpg.engine.components.TextureComponent;
import net.dp.rpg.engine.components.TransformComponent;
import net.dp.rpg.engine.systems.RenderSystem;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class RpgEngine2D extends ApplicationAdapter
{
    @Override
    public void create()
    {
        renderSystem = new RenderSystem();
        assetManager.load("eti.png", Texture.class);
        assetManager.finishLoading();
        scene = new Scene();

        eti = scene.createEntity();
        scene.addComponent(eti, new TransformComponent(new Vector2(0,0), 0));
        scene.addComponent(eti, new TextureComponent(assetManager.get("eti.png")));
        scene.addComponent(eti, new MoveComponent(new Vector2(40f, 40f)));
    }

    @Override
    public void render()
    {
        float speed = 120;
        Vector2 velocity = new Vector2(0,0);
        if(Gdx.input.isKeyPressed(Input.Keys.A))
        {
            velocity.x -= speed;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.D))
        {
            velocity.x += speed;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.W))
        {
            velocity.y += speed;
        }
        if(Gdx.input.isKeyPressed(Input.Keys.S))
        {
            velocity.y -= speed;
        }
        if(Gdx.input.isKeyJustPressed(Input.Keys.ESCAPE))
        {
            Gdx.app.exit();
        }

        scene.getComponentStorage(MoveComponent.class).getByIndex(eti).velocity = velocity;

        scene.update(Gdx.graphics.getDeltaTime());
        renderSystem.render(scene);
    }

    @Override
    public void dispose()
    {
        assetManager.dispose();
        renderSystem.dispose();
    }

    private int eti;
    private RenderSystem renderSystem;
    private final AssetManager assetManager = new AssetManager();
    private Scene scene;
}
