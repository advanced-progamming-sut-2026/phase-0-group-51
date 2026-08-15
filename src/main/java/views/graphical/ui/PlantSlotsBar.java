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
import lombok.Getter;
import lombok.Setter;
import models.App;
import models.User;
import models.games.Game;
import models.games.GameState;

import java.util.Arrays;
import java.util.HashMap;
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

    @Getter
    private PlantData selectedPlant;
    @Setter
    private Consumer<PlantData> onPlantSelected;

    private final Map<Integer, PlantCard> gameplayCardsByPlantId =
            new HashMap<>();

    private final Map<Integer, Integer> cooldownStartTickByPlantId =
            new HashMap<>();

    private final Map<Integer, Integer> cooldownEndTickByPlantId =
            new HashMap<>();

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
        gameplayCardsByPlantId.clear();

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

        if (mode == Mode.GAMEPLAY) {
            refreshGameplayAvailability();
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
            gameplayCardsByPlantId.put(
                    plant.id(),
                    card
            );

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

    @Override
    public void act(
            float delta
    ) {
        super.act(delta);

        if (mode == Mode.GAMEPLAY) {
            refreshGameplayAvailability();
        }
    }

    private void refreshGameplayAvailability() {
        Game currentGame =
                App.getInstance().getCurrentGame();

        if (currentGame == null
                || currentGame.getGameState() == null) {
            return;
        }

        GameState state =
                currentGame.getGameState();

        int currentTick =
                state.getTickCounter();

        int currentSun =
                state.getSun();

        for (PlantData plant : slots) {
            if (plant == null) {
                continue;
            }

            PlantCard card =
                    gameplayCardsByPlantId.get(
                            plant.id()
                    );

            if (card == null) {
                continue;
            }

            int cooldownEnd =
                    state.getPlantCooldownEnd(
                            plant.id()
                    );

            int ticksRemaining =
                    Math.max(
                            0,
                            cooldownEnd
                                    - currentTick
                    );

            if (ticksRemaining > 0) {
                Integer knownEnd =
                        cooldownEndTickByPlantId.get(
                                plant.id()
                        );

                if (knownEnd == null
                        || knownEnd != cooldownEnd) {
                    cooldownEndTickByPlantId.put(
                            plant.id(),
                            cooldownEnd
                    );

                    cooldownStartTickByPlantId.put(
                            plant.id(),
                            currentTick
                    );
                }

                int cooldownStart =
                        cooldownStartTickByPlantId.getOrDefault(
                                plant.id(),
                                currentTick
                        );

                int totalTicks =
                        Math.max(
                                1,
                                cooldownEnd
                                        - cooldownStart
                        );

                float fraction =
                        Math.min(
                                1f,
                                ticksRemaining
                                        / (float) totalTicks
                        );

                card.setCooldownFraction(
                        fraction
                );
            } else {
                cooldownStartTickByPlantId.remove(
                        plant.id()
                );

                cooldownEndTickByPlantId.remove(
                        plant.id()
                );

                card.setCooldownFraction(
                        0f
                );
            }

            boolean enoughSun =
                    currentSun >= plant.cost();

            boolean coolingDown =
                    ticksRemaining > 0;

            card.setEnoughSun(enoughSun);
            card.setAvailable(
                    enoughSun && !coolingDown
            );
        }
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
