package net.dp.rpg.game;

import com.badlogic.gdx.assets.loaders.ParticleEffectLoader;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.BitmapFont;
import com.badlogic.gdx.graphics.g2d.NinePatch;
import com.badlogic.gdx.graphics.g2d.ParticleEffect;
import com.badlogic.gdx.graphics.g2d.TextureAtlas;
import com.badlogic.gdx.scenes.scene2d.ui.ProgressBar;
import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.TextButton;
import com.badlogic.gdx.scenes.scene2d.utils.NinePatchDrawable;
import net.dp.rpg.engine.AbstractGame;
import net.dp.rpg.engine.Engine;
import net.dp.rpg.engine.Scene;
import net.dp.rpg.game.level.events.*;
import net.dp.rpg.game.debug.AtlasCheck;
import net.dp.rpg.game.level.gui.GameFonts;
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
        getEngine().getAssetManager().load(GameFonts.FONT, BitmapFont.class);

        ParticleEffectLoader.ParticleEffectParameter param = new ParticleEffectLoader.ParticleEffectParameter();
        param.atlasFile = atlasFile;
        getEngine().getAssetManager().load("particles/button_dust.p", ParticleEffect.class, param);
        getEngine().getAssetManager().load("particles/menu_dust.p", ParticleEffect.class, param);

        Engine.poolManager.addPool(HealthChangeEvent.class, HealthChangeEvent::new);
        Engine.poolManager.addPool(EnterRoomEvent.class, EnterRoomEvent::new);
        Engine.poolManager.addPool(SkillWindowToggleEvent.class, SkillWindowToggleEvent::new);
        Engine.poolManager.addPool(SkillPointsAvailableEvent.class, SkillPointsAvailableEvent::new);
        Engine.poolManager.addPool(SkillPointUsedEvent.class, SkillPointUsedEvent::new);
        Engine.poolManager.addPool(GameMessageEvent.class, GameMessageEvent::new);
        Engine.poolManager.addPool(ItemPickedEvent.class, ItemPickedEvent::new);
        Engine.poolManager.addPool(PotionUsedEvent.class, PotionUsedEvent::new);
        Engine.poolManager.addPool(EnemyKilledEvent.class, EnemyKilledEvent::new);
        Engine.poolManager.addPool(PlayerDamagedEvent.class, PlayerDamagedEvent::new);
        Engine.poolManager.addPool(ItemHoverEvent.class, ItemHoverEvent::new);
        Engine.poolManager.addPool(DialogueChoiceEvent.class, DialogueChoiceEvent::new);
        Engine.poolManager.addPool(DialogueEndedEvent.class, DialogueEndedEvent::new);
        Engine.poolManager.addPool(NpcActionEvent.class, NpcActionEvent::new);
        Engine.poolManager.addPool(QuestEvent.class, QuestEvent::new);
        Engine.poolManager.addPool(InteractEvent.class, InteractEvent::new);
        Engine.poolManager.addPool(PuzzleSolvedEvent.class, PuzzleSolvedEvent::new);
        Engine.poolManager.addPool(PlayerDeathEvent.class, PlayerDeathEvent::new);

        getEngine().getAssetManager().finishLoading();

        float guiButtonScale = 8;
        float healthBarScale = UiConfig.HEALTH_BAR_SCALE;
        Skin skin = getEngine().getAssetManager().get("skins/default/default.json");
        NinePatchDrawable patchDrawable = (NinePatchDrawable)skin.getDrawable("button");
        NinePatch patch = patchDrawable.getPatch();
        patch.scale(guiButtonScale, guiButtonScale);
        patchDrawable.setPatch(patch);
        patchDrawable = (NinePatchDrawable) skin.getDrawable("buttonDown");
        patch = patchDrawable.getPatch();
        patch.scale(guiButtonScale, guiButtonScale);
        patchDrawable.setPatch(patch);

        ProgressBar.ProgressBarStyle healthBarStyle = skin.get("health-bar", ProgressBar.ProgressBarStyle.class);
        patchDrawable = (NinePatchDrawable)healthBarStyle.background;
        patch = patchDrawable.getPatch();
        patch.scale(healthBarScale, healthBarScale);
        patchDrawable.setPatch(patch);
        patchDrawable = (NinePatchDrawable)healthBarStyle.knobBefore;
        patch = patchDrawable.getPatch();
        patch.scale(healthBarScale, healthBarScale);
        patchDrawable.setPatch(patch);

        getEngine().setTextureFilters(Texture.TextureFilter.Linear, Texture.TextureFilter.Nearest);
        AtlasCheck.report(atlasFile);
        GameFonts.register(getEngine().getAssetManager().get("skins/default/default.json", Skin.class),
                getEngine().getAssetManager().get(GameFonts.FONT, BitmapFont.class));

        Scene scene = getEngine().createScene();

        MenuBuilder menuBuilder = new MenuBuilder();
        menuBuilder.build(getEngine(), scene);

        getEngine().switchScene(scene);

        getEngine().setGlobalScript(new GlobalScript());

        //getEngine().setPhysicsDebug(true);
    }
}
