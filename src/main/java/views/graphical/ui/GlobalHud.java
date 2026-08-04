package views.graphical.ui;

import com.badlogic.gdx.scenes.scene2d.ui.Skin;
import com.badlogic.gdx.scenes.scene2d.ui.Table;

public final class GlobalHud extends Table {

    public GlobalHud(Skin skin) {
        setVisible(false);
    }

    public void configure(
            int coins,
            int gems,
            boolean showBackButton,
            Runnable backAction
    ) {
        // TODO: Implement the graphical HUD later.
        setVisible(false);
    }

    public void updateCurrencies(int coins, int gems) {
        // TODO: Update coin and gem labels later.
    }

    public void hideHud() {
        setVisible(false);
    }
}