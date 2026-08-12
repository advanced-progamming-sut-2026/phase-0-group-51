package views.graphical.gameplay.hud;

import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import graphics.PvzGame;
import views.graphical.ui.PlantSlotsBar;

public final class GameHud extends Table {

    private static final float PLANT_SLOTS_TOP_OFFSET = 75f;
    private static final float SIDE_PADDING = 16f;

    private final PlantSlotsBar plantSlotsBar;

    public GameHud(
            PvzGame game,
            PlantSlotsBar plantSlotsBar
    ) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        if (plantSlotsBar == null) {
            throw new IllegalArgumentException(
                    "plantSlotsBar cannot be null"
            );
        }

        this.plantSlotsBar = plantSlotsBar;

        setFillParent(true);
        top().left();

        pad(
                PLANT_SLOTS_TOP_OFFSET,
                SIDE_PADDING,
                16f,
                SIDE_PADDING
        );

        setTouchable(Touchable.childrenOnly);

        buildUi();
    }

    private void buildUi() {
        add(plantSlotsBar)
                .top()
                .left()
                .padRight(4f);
    }

    public PlantSlotsBar getPlantSlotsBar() {
        return plantSlotsBar;
    }
}
