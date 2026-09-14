package net.dp.rpg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import lombok.Getter;
import lombok.Setter;
import net.dp.rpg.engine.systems.RenderSystem;
import net.dp.rpg.game.Game;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class Engine extends ApplicationAdapter implements EngineServices
{
    private RenderSystem renderSystem;

    @Getter
    private final AssetManager assetManager = new AssetManager();

    private final AbstractGame game;
    @Setter
    private Scene activeScene;

    @Setter
    private boolean debugMode = false;
    private boolean running = true;

    public Engine()
    {
        this.game = new Game();
    }

    // --- Engine services ---

    @Override
    public Scene createScene()
    {
        return new Scene(this);
    }

    @Override
    public void exit()
    {
        running = false;
    }

    // --- Engine application ---

    @Override
    public void create()
    {
        game.setEngine(this);
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
    @Override
    public void dispose()
    {
        activeScene.dispose();
        assetManager.dispose();
        renderSystem.dispose();
    }
}
