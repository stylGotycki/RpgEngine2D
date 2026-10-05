package net.dp.rpg.engine;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.utils.PoolManager;
import net.dp.rpg.engine.systems.gameEvent.GameEventBus;

public interface EngineServices
{
    void switchScene(Scene scene);
    void setGlobalScript(Script script);
    void setPhysicsDebug(boolean enabled);
    void setGuiDebug(boolean enabled);
    void setTextureFilters(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter);
    AssetManager getAssetManager();
    GameEventBus getEventBus();
    PoolManager getPoolManager();
    Scene createScene();
    void exit();
}
