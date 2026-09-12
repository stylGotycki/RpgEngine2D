package net.dp.rpg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import lombok.Setter;
import net.dp.rpg.engine.systems.RenderSystem;
import net.dp.rpg.game.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Engine extends ApplicationAdapter
{
    public Engine()
    {
        this.game = new Game(this);
    }

    @Override
    public void create()
    {
        game.create();

        renderSystem = new RenderSystem();

        assetManager.finishLoading();
    }

    @Override
    public void render()
    {
        if(!running)
        {
            Gdx.app.exit();
            return;
        }

        activeScene.update(Gdx.graphics.getDeltaTime());

        renderSystem.render(activeScene);

        if(debugMode)
            activeScene.debugRender();
    }

    public void loadTexture(String file)
    {
        assetManager.load(file, Texture.class);
    }

    public Texture getTexture(String file)
    {
        assetManager.finishLoading();
        return assetManager.get(file);
    }

    public void exit()
    {
        running = false;
    }

    @Override
    public void dispose()
    {
        activeScene.dispose();
        assetManager.dispose();
        renderSystem.dispose();
    }

    private boolean running = true;

    private boolean debugMode = false;
    private final AbstractGame game;
    private RenderSystem renderSystem;
    private final AssetManager assetManager = new AssetManager();
    @Setter
    private Scene activeScene;
}
