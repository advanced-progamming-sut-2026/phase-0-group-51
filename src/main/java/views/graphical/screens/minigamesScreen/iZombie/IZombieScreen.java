package views.graphical.screens.minigamesScreen.iZombie;

import com.badlogic.gdx.scenes.scene2d.Actor;
import graphics.PvzGame;
import views.graphical.screens.minigamesScreen.BaseMinigameScreen;

public class IZombieScreen extends BaseMinigameScreen {
    protected IZombieScreen(PvzGame game, String backgroundLeftId, String backgroundMiddleId, String backgroundRightId) {
        super(game, backgroundLeftId, backgroundMiddleId, backgroundRightId);
    }

    @Override
    protected Actor createStartPopup(Runnable onContinue) {
        return null;
    }

    @Override
    protected void restartMinigame() {

    }

    @Override
    protected void exitMinigame() {

    }
}
