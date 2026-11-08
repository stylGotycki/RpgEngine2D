package net.dp.rpg.texturePacker;

import com.badlogic.gdx.tools.texturepacker.TexturePacker;
import com.badlogic.gdx.tools.texturepacker.TexturePacker.Settings;

public class texturePackerApplication
{
    public static void main(String[] args)
    {
        Settings settings = new Settings();
        settings.maxHeight = 512;
        settings.maxWidth = 512;
        settings.combineSubdirectories = true;
        TexturePacker.process(settings, "assets/textures-raw/", "assets/atlases/", "textures");
    }
}
