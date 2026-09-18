package net.dp.rpg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.Gdx;
import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.Array;
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

    private final AbstractGame game = new Game();
    private Scene activeScene;

    @Setter
    private Script globalScript;

    private boolean running = true;

    @Setter
    private boolean physicsDebug = false;
    private boolean guiDebug = false;

    // --- Engine services ---

    public void switchScene(Scene scene)
    {
        if(activeScene != null)
            activeScene.dispose();
        scene.getGui().getMainTable().setDebug(guiDebug);
        activeScene = scene;
    }

    @Override
    public Scene createScene()
    {
        Scene scene = new Scene(this);
        return scene;
    }

    public void setGuiDebug(boolean enabled)
    {
        guiDebug = enabled;
        if(activeScene != null)
            activeScene.getGui().getMainTable().setDebug(enabled);
    }

    @Override
    public void setTextureFilters(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter)
    {
        com.badlogic.gdx.utils.Array<Texture> textures = new Array<>();

        assetManager.getAll(Texture.class, textures);
        for(Texture texture : textures)
        {
            texture.setFilter(minFilter, magFilter);
        }
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
    public void resize(int width, int height)
    {
        activeScene.resize(width, height);
    }

    @Override
    public void render()
    {
        if(!running)
        {
            Gdx.app.exit();
            return;
        }

        if(globalScript != null) globalScript.update(Gdx.graphics.getDeltaTime());

        activeScene.update(Gdx.graphics.getDeltaTime());

        renderSystem.render(activeScene, physicsDebug, guiDebug);
    }

    @Override
    public void dispose()
    {
        activeScene.dispose();
        assetManager.dispose();
        renderSystem.dispose();
    }
}
