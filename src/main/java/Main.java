import com.badlogic.gdx.Graphics.DisplayMode;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3Application;
import com.badlogic.gdx.backends.lwjgl3.Lwjgl3ApplicationConfiguration;
import graphics.PvzGame;

public final class Main {

    private Main() {
    }

    public static void main(String[] args) {
        Lwjgl3ApplicationConfiguration config =
                new Lwjgl3ApplicationConfiguration();

        DisplayMode displayMode =
                Lwjgl3ApplicationConfiguration.getDisplayMode();

        config.setTitle("Plants vs. Zombies 2");
        config.setFullscreenMode(displayMode);
        config.useVsync(true);
        int refreshRate = displayMode.refreshRate > 0
                ? displayMode.refreshRate
                : 60;
        config.setForegroundFPS(refreshRate);
        config.setIdleFPS(30);

        new Lwjgl3Application(new PvzGame(), config);
    }
}