package net.dp.rpg.engine;

import com.badlogic.gdx.scenes.scene2d.Stage;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.utils.viewport.ScreenViewport;
import lombok.Getter;

public class Gui
{
    @Getter
    private final Stage stage = new Stage();
    @Getter
    private final Table mainTable = new Table();

    Gui()
    {
        mainTable.setFillParent(true);
        stage.addActor(mainTable);
        ScreenViewport screenViewport = new ScreenViewport();
        stage.setViewport(screenViewport);
    }

    public void update(float delta)
    {
        stage.act(delta);
    }

    public void resize(int width, int height)
    {
        stage.getViewport().update(width, height, true);
    }

    public void dispose()
    {
        stage.dispose();
    }
}
