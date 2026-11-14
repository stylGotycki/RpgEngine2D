package net.dp.rpg.demo.interior;

import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import net.dp.rpg.engine.lwjgl3.StartupHelper;

public final class InteriorDemoLauncher {

  private InteriorDemoLauncher() {
  }

  public static void main(String[] args) {
    if (StartupHelper.startNewJvmIfRequired()) {
      return;
    }

    Lwjgl3ApplicationConfiguration configuration = new Lwjgl3ApplicationConfiguration();

    configuration.setTitle("Interior demo");
    configuration.setWindowedMode(1216, 832);
    configuration.useVsync(true);
    configuration.setForegroundFPS(60);

    new Lwjgl3Application(new InteriorDemoApp(), configuration);
  }
}
