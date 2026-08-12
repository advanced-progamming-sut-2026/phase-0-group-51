package views.graphical.ui;

import Data.database.PlantBoostRepository;
import Data.database.PlantRepository;
import Data.loader.PlantData;
import com.badlogic.gdx.graphics.g2d.TextureRegion;
import com.badlogic.gdx.scenes.scene2d.Actor;
import com.badlogic.gdx.scenes.scene2d.InputEvent;
import com.badlogic.gdx.scenes.scene2d.Touchable;
import com.badlogic.gdx.scenes.scene2d.ui.ButtonGroup;
import com.badlogic.gdx.scenes.scene2d.ui.Image;
import com.badlogic.gdx.scenes.scene2d.ui.Table;
import com.badlogic.gdx.scenes.scene2d.utils.ChangeListener;
import com.badlogic.gdx.scenes.scene2d.utils.ClickListener;
import com.badlogic.gdx.scenes.scene2d.utils.TextureRegionDrawable;
import com.badlogic.gdx.utils.Scaling;
import graphics.PvzGame;
import lombok.Setter;
import models.App;
import models.User;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PlantSlotsBar extends Table {

    public enum Mode {
        SELECTION,
        GAMEPLAY
    }

    private static final int MAX_SLOTS = 8;

    private static final String EMPTY_PACKET = "IMAGE_UI_SEASONS_UNCOMPRESSED_PVZ2_SEASONS_UIASSET_PRIZE_WINDOW_UPPER_UNLOCKED";

    private static final float SLOT_SCALE = 0.90f;
    private static final float SLOT_WIDTH = 103.5f;
    private static final float SLOT_HEIGHT = 63f;

    private final PvzGame game;

    private final PlantData[] slots =
            new PlantData[MAX_SLOTS];

    private Mode mode = Mode.SELECTION;

    @Setter
    private Consumer<Integer> onRemoveRequested;
    private final ButtonGroup<PlantCard> gameplayPlantGroup =
            new ButtonGroup<>();

    private PlantData selectedPlant;
    private Consumer<PlantData> onPlantSelected;

    public PlantSlotsBar(PvzGame game) {
        if (game == null) {
            throw new IllegalArgumentException(
                    "game cannot be null"
            );
        }

        this.game = game;

        top().left();
        setTouchable(Touchable.childrenOnly);

        gameplayPlantGroup.setMinCheckCount(0);
        gameplayPlantGroup.setMaxCheckCount(1);
        gameplayPlantGroup.setUncheckLast(true);
    }

    public void loadPlants(
            List<PlantData> plants
    ) {
        Arrays.fill(slots, null);

        if (plants != null) {
            int count = Math.min(
                    plants.size(),
                    MAX_SLOTS
            );

            for (int i = 0; i < count; i++) {
                slots[i] = plants.get(i);
            }
        }

        rebuild();
    }

    public boolean contains(
            PlantData plant
    ) {
        if (plant == null) {
            return false;
        }

        for (PlantData selected : slots) {
            if (plant.equals(selected)) {
                return true;
            }
        }

        return false;
    }

    public boolean isFull() {
        for (PlantData plant : slots) {
            if (plant == null) {
                return false;
            }
        }

        return true;
    }

    public int addPlant(
            PlantData plant
    ) {
        if (plant == null || contains(plant)) {
            return -1;
        }

        for (int i = 0; i < MAX_SLOTS; i++) {
            if (slots[i] == null) {
                slots[i] = plant;
                rebuild();
                return i;
            }
        }

        return -1;
    }

    public PlantData getPlant(
            int slotIndex
    ) {
        checkSlotIndex(slotIndex);
        return slots[slotIndex];
    }

    public void removePlant(
            int slotIndex
    ) {
        checkSlotIndex(slotIndex);

        slots[slotIndex] = null;
        rebuild();
    }

    public void setMode(
            Mode mode
    ) {
        if (mode == null) {
            throw new IllegalArgumentException(
                    "mode cannot be null"
            );
        }

        if (this.mode == mode) {
            return;
        }

        this.mode = mode;
        rebuild();
    }

    private void rebuild() {
        clearChildren();
        gameplayPlantGroup.clear();

        User user = App.loggedInUser;

        Map<Integer, Integer> plantLevels =
                user == null
                        ? Map.of()
                        : PlantRepository.loadPlantLevels(
                                user.getId()
                        );

        for (int i = 0; i < MAX_SLOTS; i++) {
            PlantData plant = slots[i];

            if (plant != null) {
                add(
                        createPlantSlot(
                                plant,
                                i,
                                user,
                                plantLevels
                        )
                )
                        .size(
                                SLOT_WIDTH,
                                SLOT_HEIGHT
                        )
                        .padBottom(2f)
                        .row();

            } else if (mode == Mode.SELECTION) {
                add(createEmptySlot())
                        .size(
                                SLOT_WIDTH,
                                SLOT_HEIGHT
                        )
                        .padBottom(2f)
                        .row();
            }
        }
    }

    private PlantCard createPlantSlot(
            PlantData plant,
            int slotIndex,
            User user,
            Map<Integer, Integer> plantLevels
    ) {
        boolean boosted =
                user != null
                        && PlantBoostRepository.hasBoost(
                                user.getId(),
                                plant.id()
                        );

        int level =
                plantLevels.getOrDefault(
                        plant.id(),
                        1
                );

        PlantCard card =
                new PlantCard(
                        game,
                        new PlantCard.ViewData(
                                plant,
                                true,
                                boosted,
                                level,
                                0,
                                1,
                                true,
                                false
                        ),
                        SLOT_SCALE
                );

        if (mode == Mode.SELECTION) {
            card.addListener(
                    new ClickListener() {
                        @Override
                        public void clicked(
                                InputEvent event,
                                float x,
                                float y
                        ) {
                            if (onRemoveRequested != null) {
                                onRemoveRequested.accept(
                                        slotIndex
                                );
                            }
                        }
                    }
            );
        } else if (mode == Mode.GAMEPLAY) {
            gameplayPlantGroup.add(card);

            if (plant.equals(selectedPlant)) {
                card.setChecked(true);
            }

            card.addListener(
                    new ChangeListener() {
                        @Override
                        public void changed(
                                ChangeEvent event,
                                Actor actor
                        ) {
                            if (card.isChecked()) {
                                selectedPlant = plant;

                                if (onPlantSelected != null) {
                                    onPlantSelected.accept(
                                            plant
                                    );
                                }
                            } else if (plant.equals(selectedPlant)) {
                                selectedPlant = null;

                                if (onPlantSelected != null) {
                                    onPlantSelected.accept(
                                            null
                                    );
                                }
                            }
                        }
                    }
            );
        }

        return card;
    }

    private Image createEmptySlot() {
        TextureRegion region =
                game.getTextureBank().region(
                        EMPTY_PACKET
                );

        if (region == null) {
            throw new IllegalStateException(
                    "TextureBank region was not found: "
                            + EMPTY_PACKET
            );
        }

        Image emptySlot =
                new Image(
                        new TextureRegionDrawable(
                                region
                        )
                );

        emptySlot.setScaling(Scaling.stretch);
        emptySlot.setTouchable(
                Touchable.disabled
        );

        return emptySlot;
    }

    public void setOnPlantSelected(
            Consumer<PlantData> onPlantSelected
    ) {
        this.onPlantSelected = onPlantSelected;
    }

    public PlantData getSelectedPlant() {
        return selectedPlant;
    }

    public void clearPlantSelection() {
        selectedPlant = null;
        gameplayPlantGroup.uncheckAll();

        if (onPlantSelected != null) {
            onPlantSelected.accept(null);
        }
    }

    private void checkSlotIndex(
            int slotIndex
    ) {
        if (slotIndex < 0
                || slotIndex >= MAX_SLOTS) {
            throw new IndexOutOfBoundsException(
                    "slotIndex must be between 0 and "
                            + (MAX_SLOTS - 1)
            );
        }
    }
}
