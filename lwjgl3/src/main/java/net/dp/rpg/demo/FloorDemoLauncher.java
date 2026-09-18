package net.dp.rpg.demo;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.dp.rpg.engine.lwjgl3.StartupHelper;

public final class FloorDemoLauncher {

  private FloorDemoLauncher() {
  }

  public static void main(String[] args) {
    if (StartupHelper.startNewJvmIfRequired()) {
      return;
    }

    Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

    configuration.setTitle("Floor demo");
    configuration.setWindowedMode(1216, 832);
    configuration.useVsync(true);
    configuration.setForegroundFPS(60);

    new Lwjgl3Application(new FloorDemoApp(), configuration);
  }
}
