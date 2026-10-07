package net.dp.rpg.demo.interior;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.dp.rpg.engine.lwjgl3.StartupHelper;

public final class FloorInteriorDemoLauncher {

  private FloorInteriorDemoLauncher() {
  }

  public static void main(String[] args) {
    if (StartupHelper.startNewJvmIfRequired()) {
      return;
    }

    Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

    configuration.setTitle("Floor interior demo");
    configuration.setWindowedMode(1400, 900);
    configuration.useVsync(true);
    configuration.setForegroundFPS(60);

    new Lwjgl3Application(new FloorInteriorDemoApp(), configuration);
  }
}
