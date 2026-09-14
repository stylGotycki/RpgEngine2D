package net.dp.rpg.engine;

import com.badlogic.gdx.assets.AssetManager;

public interface EngineServices
{
    public void setActiveScene(Scene scene);
    public AssetManager getAssetManager();
    public Scene createScene();
    public void exit();
}
