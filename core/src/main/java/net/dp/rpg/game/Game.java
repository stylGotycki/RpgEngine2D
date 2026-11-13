package net.dp.rpg.game;

import com.badlogic.gdx.assets.loaders.ParticleEffectLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.utils.Drawable;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import net.dp.rpg.engine.AbstractGame;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.game.level.events.*;
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
        Engine.poolManager.addPool(PlayerDeathEvent.class, PlayerDeathEvent::new);

        getEngine().getAssetManager().finishLoading();

//        float healthBarSize = 8;
//        Skin skin = getEngine().getAssetManager().get("skins/default/default.json");
//        NinePatch healthBarPatch = skin.getPatch("health-bar");
//        NinePatch healthPatch = skin.getPatch("health");
//        healthBarPatch.scale(healthBarSize, healthBarSize);
//        healthPatch.scale(healthBarSize, healthBarSize);
//        NinePatchDrawable healthBar = (NinePatchDrawable) skin.getDrawable("health-bar");
//        NinePatchDrawable health = (NinePatchDrawable) skin.getDrawable("health");
//        healthBar.setPatch(healthBarPatch);
//        health.setPatch(healthPatch);

        getEngine().setTextureFilters(Texture.TextureFilter.Linear, Texture.TextureFilter.Nearest);

        Scene scene = getEngine().createScene();

        MenuBuilder menuBuilder = new MenuBuilder();
        menuBuilder.build(getEngine(), scene);

        getEngine().switchScene(scene);

        getEngine().setGlobalScript(new GlobalScript());

        //getEngine().setPhysicsDebug(true);
    }
}
