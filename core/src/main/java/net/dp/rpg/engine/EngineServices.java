package net.dp.rpg.engine;

import com.badlogic.gdx.assets.AssetManager;
import com.badlogic.gdx.graphics.Texture;

public interface EngineServices
{
    void switchScene(Scene scene);
    void setGlobalScript(Script script);
    void setPhysicsDebug(boolean enabled);
    void setGuiDebug(boolean enabled);
    void setTextureFilters(Texture.TextureFilter minFilter, Texture.TextureFilter magFilter);
    AssetManager getAssetManager();
    Scene createScene();
    void exit();
}
