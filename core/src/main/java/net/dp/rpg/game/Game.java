package net.dp.rpg.game;

import com.badlogic.gdx.assets.loaders.ParticleEffectLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import net.dp.rpg.engine.AbstractGame;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.game.events.*;
import net.dp.rpg.game.menu.MenuBuilder;
import net.dp.rpg.game.scripts.GlobalScript;

public class Game extends AbstractGame
{
    @Override
    public void create()
    {
        String atlasFile = "atlases/textures.atlas";
        getEngine().getAssetManager().load(atlasFile, TextureAtlas.class);
        getEngine().getAssetManager().load("skins/default/default.json", Skin.class);

        ParticleEffectLoader.ParticleEffectParameter param = new ParticleEffectLoader.ParticleEffectParameter();
        param.atlasFile = atlasFile;
        getEngine().getAssetManager().load("particles/button_dust.p", ParticleEffect.class, param);
        getEngine().getAssetManager().load("particles/menu_dust.p", ParticleEffect.class, param);

        Engine.poolManager.addPool(HealthChangeEvent.class, HealthChangeEvent::new);
        Engine.poolManager.addPool(EnterRoomEvent.class, EnterRoomEvent::new);
        Engine.poolManager.addPool(SkillWindowToggleEvent.class, SkillWindowToggleEvent::new);
        Engine.poolManager.addPool(SkillPointsAvailableEvent.class, SkillPointsAvailableEvent::new);
        Engine.poolManager.addPool(SkillPointUsedEvent.class, SkillPointUsedEvent::new);

        getEngine().getAssetManager().finishLoading();

        getEngine().setTextureFilters(Texture.TextureFilter.Linear, Texture.TextureFilter.Nearest);

        Scene scene = getEngine().createScene();

        MenuBuilder menuBuilder = new MenuBuilder();
        menuBuilder.build(getEngine(), scene);

        getEngine().switchScene(scene);

        getEngine().setGlobalScript(new GlobalScript());

        //getEngine().setPhysicsDebug(true);
    }
}
