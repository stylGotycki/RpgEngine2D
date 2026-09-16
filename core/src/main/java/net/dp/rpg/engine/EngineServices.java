package net.dp.rpg.engine;

import com.badlogic.gdx.assets.AssetManager;

public interface EngineServices
{
    void switchScene(Scene scene);
    void setPhysicsDebug(boolean enabled);
    void setGuiDebug(boolean enabled);
    AssetManager getAssetManager();
    Scene createScene();
    void exit();
}
