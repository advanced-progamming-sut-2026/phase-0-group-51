package models.Plant;

import Data.loader.PlantData;
import Data.loader.PlantRegistry;
import models.games.GameState;

import java.util.Locale;

public enum Mint implements PlantType {
    ENLIGHTEN_MINT(61, "sunproducer"),
    APPEASE_MINT(62, "shooter"),
    ARMA_MINT(63, "lobber"),
    BOMBARD_MINT(64, "explosive"),
    ENFORCE_MINT(65, "melee"),
    REINFORCE_MINT(66, "wallnut"),
    ENCHANT_MINT(67, "modifier"),
    PIERCE_MINT(68, "strikethrough"),
    CAT_TAIL_MINT(69, "homing");

    private static final int BASE_DURATION_SECONDS = 1;
    private static final int FIRST_MINT_ID = 61;
    private static final int LAST_MINT_ID = 69;

    private final int id;
    private final String family;

    Mint(int id, String family) {
        this.id = id;
        this.family = family;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        int seconds = BASE_DURATION_SECONDS
                + (plant.getLevel() >= 2 ? 1 : 0);
        int durationTicks = seconds * state.getTicksPerSecond();
        for (Plant familyPlant : state.getBoard().getAllPlants()) {
            if (familyPlant == plant || !belongsToFamily(familyPlant)) {
                continue;
            }
            familyPlant.feedForAtLeast(state, durationTicks);
        }
        if (plant.getLevel() >= 4) {
            for (PlantData data : PlantRegistry.getAll()) {
                if (belongsToFamily(data)) {
                    state.resetPlantCooldown(data.id());
                }
            }
        }
        plant.setMarkedForRemoval(true);
        state.getBoard().removePlant(plant);
        state.logEvent(plant.getName() + " activated the " + family
                + " family for " + seconds + " seconds.\n");
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        // Instant-use plant.
    }

    @Override
    public int plantFoodDurationTicks(Plant plant, GameState state) {
        return 0;
    }

    private boolean belongsToFamily(Plant plant) {
        String runtimeFamily = familyForType(plant.getPlantType());
        if (!runtimeFamily.isEmpty()) {
            return runtimeFamily.equals(family);
        }
        PlantData data = PlantRegistry.getById(plant.getId());
        return data != null && belongsToFamily(data);
    }

    private static String familyForType(PlantType type) {
        if (type instanceof SunProducer) {
            return "sunproducer";
        }
        if (type instanceof Shooter) {
            return "shooter";
        }
        if (type instanceof Lobber) {
            return "lobber";
        }
        if (type instanceof Explosive) {
            return "explosive";
        }
        if (type instanceof Melee) {
            return "melee";
        }
        if (type instanceof WallNut) {
            return "wallnut";
        }
        if (type instanceof Modifier) {
            return "modifier";
        }
        if (type instanceof StrikeThrough) {
            return "strikethrough";
        }
        if (type instanceof Homing) {
            return "homing";
        }
        return "";
    }

    private boolean belongsToFamily(PlantData data) {
        if (data.id() >= FIRST_MINT_ID && data.id() <= LAST_MINT_ID) {
            return false;
        }
        return normalizeCategory(data.category()).equals(family);
    }

    private static String normalizeCategory(String value) {
        return value == null ? ""
                : value.replaceAll("[^A-Za-z]", "")
                .toLowerCase(Locale.ROOT);
    }
}
