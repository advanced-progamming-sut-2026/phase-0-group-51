package models.Plant;

import models.games.GameState;
import models.sun.Sun;
import models.sun.SunType;

public enum SunProducer implements PlantType {
    SUNFLOWER(1, 50, false),
    TWIN_SUNFLOWER(2, 100, false),
    PRIMAL_SUNFLOWER(4, 75, false),
    GOLD_BLOOM(5, 375, true);

    private static final int DOUBLE_SUN_CHANCE_PERCENT = 25;
    private final int id;
    private final int baseSunAmount;
    private final boolean immediate;

    SunProducer(int id, int baseSunAmount, boolean immediate) {
        this.id = id;
        this.baseSunAmount = baseSunAmount;
        this.immediate = immediate;
    }

    public Plant create() {
        return PlantEnumSupport.create(id, this);
    }

    @Override
    public void onPlanted(Plant plant, GameState state) {
        if (!immediate) {
            return;
        }
        int amount = baseSunAmount + Math.max(0, plant.getPlantStat().sunAmount() - 50);
        state.increaseSunBalance(amount);
        state.logEvent(plant.getName() + " immediately produced " + amount + " suns.\n");
        removeAfterActing(plant, state);
    }

    @Override
    public void onTick(Plant plant, GameState state) {
        if (!immediate) {
            produceCollectableSun(plant, state, baseSunAmount);
        }
    }

    @Override
    public void onFeed(Plant plant, GameState state) {
        if (immediate) {
            return;
        }
        state.increaseSunBalance(Math.max(150, baseSunAmount * 3));
        state.logEvent(plant.getName() + " plant food produced bonus suns.\n");
    }

    private static void produceCollectableSun(
            Plant plant,
            GameState state,
            int baseAmount
    ) {
        if (plant.isPendingSun()) {
            return;
        }
        int amount = baseAmount;
        if (shouldProduceDoubleSun(plant, state)) {
            amount *= 2;
        }
        plant.setPendingSun(true);
        state.getBoard().spawnSun(new Sun(
                plant.getPosX(),
                plant.getPosY(),
                plant.getPosY(),
                SunType.ORDINARY,
                amount,
                Integer.MAX_VALUE,
                plant
        ));
        state.logEvent(plant.getName() + " produced a sun at ("
                + (plant.getPosX() + 1) + ", " + (plant.getPosY() + 1) + ")\n");
    }

    private static boolean shouldProduceDoubleSun(Plant plant, GameState state) {
        return plant.getPlantStat().doubleSunChance()
                && state.getBoard().getRandom().nextInt(100) < DOUBLE_SUN_CHANCE_PERCENT;
    }

    private static void removeAfterActing(Plant plant, GameState state) {
        plant.setMarkedForRemoval(true);
        state.getBoard().removePlant(plant.getPosY(), plant.getPosX());
    }
}
