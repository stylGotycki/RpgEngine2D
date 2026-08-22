package net.dp.rpg.engine;

import com.badlogic.gdx.ApplicationAdapter;
import com.badlogic.gdx.graphics.Texture;
import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.utils.ScreenUtils;

/** {@link com.badlogic.gdx.ApplicationListener} implementation shared by all platforms. */
public class RpgEngine2D extends ApplicationAdapter {
    private SpriteBatch batch;
    private Texture image;
    private final int WIDTH = 640, HEIGHT = 480;
    private int x = 0, y = 0;
    private int dx = 1, dy = 1;

    @Override
    public void create() {
        batch = new SpriteBatch();
        image = new Texture("eti.png");
        x = WIDTH - image.getWidth();
        y = HEIGHT - image.getHeight();
    }

    @Override
    public void render() {
        ScreenUtils.clear(0.15f, 0.15f, 0.2f, 1f);
        batch.begin();
        drawImage();
        batch.end();
    }

    @Override
    public void dispose() {
        batch.dispose();
        image.dispose();
    }

    private void drawImage() {
        batch.draw(image, x, y);

        if (x > WIDTH - image.getWidth()) dx = -1;
        else if (x < 0) dx = 1;

        if (y > HEIGHT - image.getHeight()) dy = -1;
        else if (y < 0) dy = 1;

        x += dx;
        y += dy;
    }
}
